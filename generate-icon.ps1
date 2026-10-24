Add-Type -AssemblyName System.Drawing

$srcPath = (Resolve-Path "src\main\resources\com\kainanresto\images\id\KainanNiJuanLogoTransparent.png").Path
$srcBmp = [System.Drawing.Bitmap]::FromFile($srcPath)

function Generate-CircularBadgeBmp([int]$size) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    $pad = [Math]::Max(1.0, $size * 0.02)
    $circleSize = $size - (2.0 * $pad)

    # Background circle
    $brushBg = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 252, 249, 242))
    $g.FillEllipse($brushBg, [float]$pad, [float]$pad, [float]$circleSize, [float]$circleSize)
    $brushBg.Dispose()

    # Border
    $borderW = [Math]::Max(1.0, $size * 0.02)
    $penBorder = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 218, 198, 172), [float]$borderW)
    $g.DrawEllipse($penBorder, [float]$pad, [float]$pad, [float]$circleSize, [float]$circleSize)
    $penBorder.Dispose()

    # Emblem cropped: center (1000, 1000), size 1360 x 1360
    # Inside badge with ~10% margin
    $emblemPad = [Math]::Round($size * 0.10)
    $emblemSize = $size - (2 * $emblemPad)
    $destRect = New-Object System.Drawing.Rectangle($emblemPad, $emblemPad, $emblemSize, $emblemSize)
    $g.DrawImage($srcBmp, $destRect, 320, 320, 1360, 1360, [System.Drawing.GraphicsUnit]::Pixel)

    $g.Dispose()
    return $bmp
}

# 1. Save 256x256 and 64x64 PNG in resources
$resBmp256 = Generate-CircularBadgeBmp 256
$resBmp256.Save("src\main\resources\com\kainanresto\images\id\app-icon.png", [System.Drawing.Imaging.ImageFormat]::Png)
$resBmp256.Dispose()

# 2. Build multi-resolution ICO
$sizes = @(16, 24, 32, 48, 64, 128, 256)
$images = @()

foreach ($s in $sizes) {
    $bmp = Generate-CircularBadgeBmp $s
    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    $bytes = $ms.ToArray()
    $ms.Dispose()

    $images += [PSCustomObject]@{
        Width = $s
        Height = $s
        Bytes = $bytes
    }
}
$srcBmp.Dispose()

$icoPath = "app-icon.ico"
$fs = [System.IO.File]::Create($icoPath)
$bw = New-Object System.IO.BinaryWriter($fs)

# ICONDIR
$bw.Write([uint16]0)
$bw.Write([uint16]1)
$bw.Write([uint16]$images.Count)

$offset = 6 + ($images.Count * 16)
foreach ($img in $images) {
    $w = if ($img.Width -ge 256) { 0 } else { [byte]$img.Width }
    $h = if ($img.Height -ge 256) { 0 } else { [byte]$img.Height }
    $bw.Write([byte]$w)
    $bw.Write([byte]$h)
    $bw.Write([byte]0)
    $bw.Write([byte]0)
    $bw.Write([uint16]1)
    $bw.Write([uint16]32)
    $bw.Write([uint32]$img.Bytes.Length)
    $bw.Write([uint32]$offset)
    $offset += $img.Bytes.Length
}

foreach ($img in $images) {
    $bw.Write($img.Bytes)
}

$bw.Flush()
$bw.Close()
$fs.Close()

Write-Output "Successfully generated app-icon.ico and app-icon.png"
