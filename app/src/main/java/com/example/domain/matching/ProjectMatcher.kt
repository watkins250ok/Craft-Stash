package com.example.domain.matching

import com.example.data.dao.ProjectWithDetails
import com.example.data.dao.SupplyWithBatches
import com.example.data.entity.BatchEntity
import com.example.data.entity.ProjectAllocationEntity
import com.example.data.entity.ProjectRequirementEntity
import com.example.domain.measurement.UnitConverter
import com.example.domain.model.CoverageState
import com.example.domain.model.MatchStatus
import com.example.domain.model.QuantityMode

data class BatchAvailability(
    val batch: BatchEntity,
    val onHand: Double?,
    val reserved: Double,
    val available: Double?,
    val isOvercommitted: Boolean,
    val reservedByProjects: List<String>
)

data class RequirementMatchEvaluation(
    val requirement: ProjectRequirementEntity,
    val coverageState: CoverageState,
    val matchedAllocations: List<ProjectAllocationEntity>,
    val explanation: String,
    val totalCoveredQuantity: Double?,
    val totalRequiredQuantity: Double?
)

data class ProjectMatchResult(
    val projectWithDetails: ProjectWithDetails,
    val requirementEvaluations: List<RequirementMatchEvaluation>,
    val isReadyToMake: Boolean,
    val confirmedMatchedSupplies: List<String>,
    val matchedExplanations: List<String>,
    val missingCount: Int,
    val unknownCount: Int,
    val reviewNeededCount: Int,
    val coverageSummary: String
)

object ProjectMatcher {

    /**
     * Calculates the availability of a batch taking all existing allocations into account.
     */
    fun calculateBatchAvailability(
        batch: BatchEntity,
        allAllocations: List<ProjectAllocationEntity>
    ): BatchAvailability {
        if (batch.isDepleted) {
            return BatchAvailability(
                batch = batch,
                onHand = 0.0,
                reserved = 0.0,
                available = 0.0,
                isOvercommitted = false,
                reservedByProjects = emptyList()
            )
        }

        val batchAllocations = allAllocations.filter { it.batchId == batch.id }
        var totalReserved = 0.0
        val projectIds = mutableListOf<String>()

        for (alloc in batchAllocations) {
            projectIds.add(alloc.projectId)
            val resQty = alloc.reservedQuantity
            if (resQty != null && resQty > 0) {
                // If units match or can be converted to batch unit
                if (batch.unit.isNullOrBlank() || alloc.reservedUnit.isNullOrBlank() || batch.unit.equals(alloc.reservedUnit, ignoreCase = true)) {
                    totalReserved += resQty
                } else {
                    val converted = UnitConverter.convert(resQty, alloc.reservedUnit, batch.unit)
                    if (converted != null) {
                        totalReserved += converted
                    } else {
                        // Incompatible unit reservation, keep raw number
                        totalReserved += resQty
                    }
                }
            }
        }

        val onHand = batch.quantityValue
        val available = if (onHand != null && batch.quantityMode == QuantityMode.EXACT) {
            maxOf(0.0, onHand - totalReserved)
        } else if (onHand != null && batch.quantityMode == QuantityMode.APPROXIMATE) {
            maxOf(0.0, onHand - totalReserved)
        } else {
            null // presence only or untracked
        }

        val isOvercommitted = onHand != null && totalReserved > onHand

        return BatchAvailability(
            batch = batch,
            onHand = onHand,
            reserved = totalReserved,
            available = available,
            isOvercommitted = isOvercommitted,
            reservedByProjects = projectIds.distinct()
        )
    }

    /**
     * Evaluates a project's requirements against its allocations and the supplies in the stash.
     */
    fun evaluateProject(
        projectWithDetails: ProjectWithDetails,
        allSuppliesWithBatches: List<SupplyWithBatches>,
        allAllocations: List<ProjectAllocationEntity>
    ): ProjectMatchResult {
        val reqs = projectWithDetails.requirements
        val allocations = projectWithDetails.allocations
        val batchMap = allSuppliesWithBatches.flatMap { it.batches }.associateBy { it.id }
        val supplyMap = allSuppliesWithBatches.associateBy { it.supply.id }

        val evaluations = mutableListOf<RequirementMatchEvaluation>()
        val matchedSupplies = mutableListOf<String>()
        val explanations = mutableListOf<String>()

        var missingCount = 0
        var unknownCount = 0
        var reviewNeededCount = 0

        for (req in reqs) {
            val reqAllocations = allocations.filter { it.requirementId == req.id }
            if (reqAllocations.isEmpty()) {
                if (req.isRequired) {
                    missingCount++
                    val missingExpl = "Missing: ${req.description}"
                    explanations.add(missingExpl)
                    evaluations.add(
                        RequirementMatchEvaluation(
                            requirement = req,
                            coverageState = CoverageState.MISSING,
                            matchedAllocations = emptyList(),
                            explanation = missingExpl,
                            totalCoveredQuantity = null,
                            totalRequiredQuantity = req.quantityRequired
                        )
                    )
                }
                continue
            }

            // Check match status of allocations
            var hasSubstitute = false
            var totalCovered = 0.0
            var hasUntracked = false

            for (alloc in reqAllocations) {
                if (alloc.matchStatus == MatchStatus.POSSIBLE_SUBSTITUTE) {
                    hasSubstitute = true
                }
                val batch = batchMap[alloc.batchId]
                if (batch != null) {
                    val supply = supplyMap[batch.supplyId]
                    if (supply != null) {
                        matchedSupplies.add(supply.supply.name)
                    }

                    if (batch.quantityMode == QuantityMode.PRESENCE_ONLY || batch.quantityValue == null) {
                        hasUntracked = true
                    } else {
                        val avail = calculateBatchAvailability(batch, allAllocations)
                        val allocQty = alloc.reservedQuantity ?: avail.available ?: 0.0
                        if (alloc.reservedUnit.isNullOrBlank() || req.unit.isNullOrBlank() || alloc.reservedUnit.equals(req.unit, ignoreCase = true)) {
                            totalCovered += allocQty
                        } else {
                            val converted = UnitConverter.convert(allocQty, alloc.reservedUnit, req.unit)
                            if (converted != null) {
                                totalCovered += converted
                            } else {
                                totalCovered += allocQty
                            }
                        }

                        // Check single continuous piece requirement
                        if (req.singleContinuousPieceRequired) {
                            val fitsPiece = UnitConverter.canPieceSatisfyDimensions(
                                availW = batch.dimensionWidth,
                                availL = batch.dimensionLength,
                                reqW = req.dimensionWidthRequired,
                                reqL = req.dimensionLengthRequired
                            )
                            if (!fitsPiece) {
                                hasSubstitute = true
                            }
                        }
                    }
                }
            }

            val coverageState = when {
                hasSubstitute -> {
                    reviewNeededCount++
                    CoverageState.SUBSTITUTE_REVIEW_NEEDED
                }
                hasUntracked -> {
                    unknownCount++
                    CoverageState.QUANTITY_UNKNOWN
                }
                req.quantityRequired == null -> {
                    CoverageState.HAVE_ENOUGH
                }
                totalCovered >= req.quantityRequired -> {
                    CoverageState.HAVE_ENOUGH
                }
                totalCovered > 0 -> {
                    CoverageState.HAVE_SOME
                }
                else -> {
                    if (req.isRequired) missingCount++
                    CoverageState.MISSING
                }
            }

            val expl = when (coverageState) {
                CoverageState.HAVE_ENOUGH -> "Confirmed: ${req.description}"
                CoverageState.HAVE_SOME -> "Partial: ${UnitConverter.formatQuantity(totalCovered)} of ${UnitConverter.formatQuantity(req.quantityRequired)} ${req.unit.orEmpty()}"
                CoverageState.QUANTITY_UNKNOWN -> "Quantity untracked for ${req.description}"
                CoverageState.SUBSTITUTE_REVIEW_NEEDED -> "Review needed: substitute or piece dimension check for ${req.description}"
                CoverageState.MISSING -> "Missing: ${req.description}"
            }
            explanations.add(expl)

            evaluations.add(
                RequirementMatchEvaluation(
                    requirement = req,
                    coverageState = coverageState,
                    matchedAllocations = reqAllocations,
                    explanation = expl,
                    totalCoveredQuantity = totalCovered,
                    totalRequiredQuantity = req.quantityRequired
                )
            )
        }

        val requiredCount = reqs.count { it.isRequired }
        val coveredCount = evaluations.count { it.requirement.isRequired && it.coverageState == CoverageState.HAVE_ENOUGH }

        val isReadyToMake = requiredCount > 0 && coveredCount == requiredCount && reviewNeededCount == 0 && unknownCount == 0

        val coverageSummary = if (reqs.isEmpty()) {
            "No materials entered yet (coverage applies to entered requirements)."
        } else if (isReadyToMake) {
            "Ready to make: All $requiredCount entered requirements confirmed!"
        } else {
            "$coveredCount of $requiredCount entered materials confirmed (${missingCount} missing, ${unknownCount} untracked)."
        }

        return ProjectMatchResult(
            projectWithDetails = projectWithDetails,
            requirementEvaluations = evaluations,
            isReadyToMake = isReadyToMake,
            confirmedMatchedSupplies = matchedSupplies.distinct(),
            matchedExplanations = explanations,
            missingCount = missingCount,
            unknownCount = unknownCount,
            reviewNeededCount = reviewNeededCount,
            coverageSummary = coverageSummary
        )
    }

    /**
     * Matches saved projects to selected supplies.
     * Supports matchAny (use ANY selected supply) and matchAll (try to use ALL selected supplies).
     */
    fun matchProjectsForSupplies(
        selectedSupplyIds: Set<String>,
        allProjects: List<ProjectWithDetails>,
        allSuppliesWithBatches: List<SupplyWithBatches>,
        allAllocations: List<ProjectAllocationEntity>,
        matchAll: Boolean = false,
        includeCompletedOrArchived: Boolean = false
    ): List<ProjectMatchResult> {
        val supplyMap = allSuppliesWithBatches.associateBy { it.supply.id }
        val selectedSupplies = selectedSupplyIds.mapNotNull { supplyMap[it] }
        val selectedBatchIds = selectedSupplies.flatMap { it.batches }.map { it.id }.toSet()

        val filteredProjects = allProjects.filter { p ->
            if (includeCompletedOrArchived) true
            else p.project.status != com.example.domain.model.ProjectStatus.COMPLETED &&
                    p.project.status != com.example.domain.model.ProjectStatus.ARCHIVED
        }

        val results = mutableListOf<ProjectMatchResult>()

        for (project in filteredProjects) {
            val eval = evaluateProject(project, allSuppliesWithBatches, allAllocations)

            if (selectedSupplyIds.isEmpty()) {
                results.add(eval)
                continue
            }

            // Check which selected supplies link to this project
            val projectAllocatedBatchIds = project.allocations.map { it.batchId }.toSet()
            val linkedSelectedBatchIds = projectAllocatedBatchIds.intersect(selectedBatchIds)
            val linkedSupplyIds = linkedSelectedBatchIds.mapNotNull { bId ->
                allSuppliesWithBatches.find { s -> s.batches.any { it.id == bId } }?.supply?.id
            }.toSet()

            if (matchAll) {
                if (linkedSupplyIds.containsAll(selectedSupplyIds)) {
                    results.add(eval)
                }
            } else {
                if (linkedSupplyIds.isNotEmpty()) {
                    results.add(eval)
                }
            }
        }

        // Rank results: Ready to make first, then most covered, then fewest missing
        return results.sortedWith(
            compareByDescending<ProjectMatchResult> { it.isReadyToMake }
                .thenByDescending { it.confirmedMatchedSupplies.size }
                .thenBy { it.missingCount }
                .thenBy { it.reviewNeededCount }
        )
    }
}
