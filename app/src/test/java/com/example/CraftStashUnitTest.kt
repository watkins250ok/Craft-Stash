package com.example

import com.example.data.dao.ProjectWithDetails
import com.example.data.dao.SupplyWithBatches
import com.example.data.entity.*
import com.example.domain.inspiration.InspirationEngine
import com.example.domain.matching.ProjectMatcher
import com.example.domain.measurement.UnitConverter
import com.example.domain.model.CoverageState
import com.example.domain.model.MatchStatus
import com.example.domain.model.ProjectStatus
import com.example.domain.model.QuantityMode
import org.junit.Assert.*
import org.junit.Test

class CraftStashUnitTest {

    @Test
    fun testQuantityConversions_compatibleUnits() {
        // Mass: grams to oz
        val oz = UnitConverter.convert(28.349523125, "g", "oz")
        assertNotNull(oz)
        assertEquals(1.0, oz!!, 0.001)

        // Length: yards to meters
        val meters = UnitConverter.convert(1.0, "yard", "meter")
        assertNotNull(meters)
        assertEquals(0.9144, meters!!, 0.001)

        // Volume: liters to ml
        val ml = UnitConverter.convert(0.5, "liter", "ml")
        assertNotNull(ml)
        assertEquals(500.0, ml!!, 0.001)
    }

    @Test
    fun testIncompatibleUnitRejection() {
        // Grams to meters must be rejected
        val invalid1 = UnitConverter.convert(100.0, "g", "meters")
        assertNull("Mass cannot convert to length without item-specific info", invalid1)

        // Fl oz to yards must be rejected
        val invalid2 = UnitConverter.convert(5.0, "fl oz", "yd")
        assertNull("Volume cannot convert to length", invalid2)

        assertFalse(UnitConverter.areUnitsCompatible("grams", "meters"))
        assertTrue(UnitConverter.areUnitsCompatible("meters", "cm"))
        assertTrue(UnitConverter.areUnitsCompatible("g", "oz"))
    }

    @Test
    fun testUnknownVersusZero() {
        // Unknown quantity is not zero!
        val presenceDisplay = UnitConverter.formatDisplayQuantity(null, null, QuantityMode.PRESENCE_ONLY)
        assertEquals("On hand (untracked)", presenceDisplay)

        val approxDisplay = UnitConverter.formatDisplayQuantity(null, null, QuantityMode.APPROXIMATE)
        assertEquals("~Some", approxDisplay)

        val zeroExact = UnitConverter.formatDisplayQuantity(0.0, "g", QuantityMode.EXACT)
        assertEquals("0 g", zeroExact)
        assertNotEquals(presenceDisplay, zeroExact)
    }

    @Test
    fun testReservationAndAvailabilityCalculations() {
        val batch = BatchEntity(
            id = "batch_1",
            supplyId = "supply_1",
            batchIdentifier = "Dye Lot A",
            quantityMode = QuantityMode.EXACT,
            quantityValue = 100.0,
            unit = "g"
        )

        val allocations = listOf(
            ProjectAllocationEntity(
                id = "alloc_1",
                projectId = "proj_1",
                requirementId = "req_1",
                batchId = "batch_1",
                reservedQuantity = 30.0,
                reservedUnit = "g"
            ),
            ProjectAllocationEntity(
                id = "alloc_2",
                projectId = "proj_2",
                requirementId = "req_2",
                batchId = "batch_1",
                reservedQuantity = 20.0,
                reservedUnit = "g"
            )
        )

        val avail = ProjectMatcher.calculateBatchAvailability(batch, allocations)
        assertEquals(100.0, avail.onHand!!, 0.001)
        assertEquals(50.0, avail.reserved, 0.001)
        assertEquals(50.0, avail.available!!, 0.001)
        assertFalse(avail.isOvercommitted)
        assertEquals(2, avail.reservedByProjects.size)

        // Test overcommitment warning
        val overcommittedAllocations = allocations + ProjectAllocationEntity(
            id = "alloc_3",
            projectId = "proj_3",
            requirementId = "req_3",
            batchId = "batch_1",
            reservedQuantity = 60.0,
            reservedUnit = "g"
        )
        val overcommittedAvail = ProjectMatcher.calculateBatchAvailability(batch, overcommittedAllocations)
        assertTrue("Total reserved (110g) exceeds on-hand (100g)", overcommittedAvail.isOvercommitted)
    }

    @Test
    fun testSinglePieceFabricConstraints() {
        // Piece A is 24x36 inches
        val pieceWidth = 24.0
        val pieceLength = 36.0

        // Requirement needing 20x30 fits (normal orientation)
        assertTrue(UnitConverter.canPieceSatisfyDimensions(pieceWidth, pieceLength, 20.0, 30.0))

        // Requirement needing 35x22 fits (rotated orientation)
        assertTrue(UnitConverter.canPieceSatisfyDimensions(pieceWidth, pieceLength, 35.0, 22.0))

        // Requirement needing 20x40 DOES NOT fit even though area (800 sq in) is less than piece area (864 sq in)!
        assertFalse(
            "Distinct pieces cannot be claimed equivalent to area without meeting dimensions",
            UnitConverter.canPieceSatisfyDimensions(pieceWidth, pieceLength, 20.0, 40.0)
        )
    }

    @Test
    fun testBarcodePreservesLeadingZeros() {
        val barcode = "0001234567890"
        val supply = SupplyEntity(
            id = "sup_1",
            name = "Test Thread",
            category = "Thread",
            craftTypes = "Sewing",
            barcode = barcode
        )
        assertEquals("0001234567890", supply.barcode)
        assertTrue(supply.barcode.startsWith("000"))
    }

    @Test
    fun testReusableToolsAllocation() {
        val toolReq = ProjectRequirementEntity(
            id = "req_tool",
            projectId = "proj_1",
            description = "5.0mm Knitting Needles",
            isReusableTool = true,
            isRequired = true
        )
        assertTrue(toolReq.isReusableTool)
    }

    @Test
    fun testLocalQueryGeneration_excludesPrivateNotesAndLocations() {
        val supply = SupplyWithBatches(
            supply = SupplyEntity(
                id = "sup_test",
                name = "Vintage Floral Fabric",
                category = "Fabric",
                craftTypes = "Sewing",
                material = "Cotton",
                notes = "SECRET PRIVATE NOTE: bought at thrift store for 5 dollars"
            ),
            batches = listOf(
                BatchEntity(
                    id = "batch_test",
                    supplyId = "sup_test",
                    condition = "Remnants",
                    storageLocationId = "secret_safe_box_id",
                    notes = "PRIVATE BATCH NOTE: under the bed"
                )
            )
        )

        val query = InspirationEngine.generateSearchQuery(null, listOf(supply), "Sewing")

        assertFalse("Query must NOT leak private notes", query.contains("thrift"))
        assertFalse("Query must NOT leak location notes", query.contains("bed"))
        assertFalse("Query must NOT leak internal IDs", query.contains("secret"))
        assertTrue("Query should include general craft terms", query.contains("sewing") || query.contains("cotton"))
    }

    @Test
    fun testProjectMatchingExplanations() {
        val project = ProjectWithDetails(
            project = ProjectEntity(
                id = "proj_1",
                title = "Test Quilt",
                status = ProjectStatus.PLANNED
            ),
            requirements = listOf(
                ProjectRequirementEntity(
                    id = "req_1",
                    projectId = "proj_1",
                    description = "Quilt Cotton",
                    quantityRequired = 2.0,
                    unit = "yards",
                    isRequired = true
                )
            ),
            allocations = emptyList()
        )

        val eval = ProjectMatcher.evaluateProject(project, emptyList(), emptyList())
        assertEquals(1, eval.missingCount)
        assertFalse(eval.isReadyToMake)
        assertTrue(eval.matchedExplanations.first().startsWith("Missing: Quilt Cotton"))
    }

    @Test
    fun testThemeModeValues() {
        assertEquals("System", com.example.domain.model.ThemeMode.SYSTEM.displayName)
        assertEquals("Light", com.example.domain.model.ThemeMode.LIGHT.displayName)
        assertEquals("Dark", com.example.domain.model.ThemeMode.DARK.displayName)
        assertEquals(com.example.domain.model.ThemeMode.DARK, com.example.domain.model.ThemeMode.valueOf("DARK"))
    }
}
