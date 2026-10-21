#!/usr/bin/env bash
set -e

SRC="./tmp/craft_icon.png"
BASE="./app/src/main/res"

# Generate master 512x512 craft icon
convert -size 512x512 xc:'#4A2814' \
  -fill none -stroke '#724328' -strokewidth 8 -draw "roundrectangle 24,24 488,488 48,48" \
  -stroke none \
  -fill '#FFE082' -stroke '#F4A261' -strokewidth 12 -draw "line 110,110 402,402" \
  -draw "line 402,110 110,402" \
  -fill '#F4A261' -stroke none -draw "circle 110,110 110,92" -draw "circle 402,110 402,92" \
  -fill '#E07A5F' -stroke '#3B1E0E' -strokewidth 6 -draw "circle 256,265 256,145" \
  -fill none -stroke '#F4A261' -strokewidth 10 -draw "arc 160,180 352,350 30,150" \
  -stroke '#FFD166' -strokewidth 8 -draw "arc 170,195 342,335 210,330" \
  -stroke '#E76F51' -strokewidth 10 -draw "arc 180,210 332,320 0,180" \
  -stroke '#F4A261' -strokewidth 8 -draw "bezier 340,320 380,360 410,390 430,370" \
  -fill '#F4F1DE' -stroke '#3B1E0E' -strokewidth 4 -draw "circle 256,390 256,366" \
  -fill '#3B1E0E' -stroke none \
  -draw "circle 250,384 250,380" \
  -draw "circle 262,384 262,380" \
  -draw "circle 250,396 250,392" \
  -draw "circle 262,396 262,392" \
  PNG32:"$SRC"

echo "Master icon created at $SRC"

# Generate all mipmap folders
for spec in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
  dir="$BASE/mipmap-${spec%:*}"
  size="${spec#*:}"
  convert "$SRC" -resize "${size}x${size}!" "PNG32:$dir/ic_launcher.png"
  [ -f "$dir/ic_launcher.png" ] && rm -f "$dir/ic_launcher.webp"
  radius=$((size / 2))
  convert "$SRC" -resize "${size}x${size}!" \
    \( -size "${size}x${size}" xc:none -fill white \
       -draw "circle $radius,$radius $radius,0" \) \
    -alpha set -compose DstIn -composite "PNG32:$dir/ic_launcher_round.png"
  [ -f "$dir/ic_launcher_round.png" ] && rm -f "$dir/ic_launcher_round.webp"
done

echo "Mipmap icons updated successfully!"
