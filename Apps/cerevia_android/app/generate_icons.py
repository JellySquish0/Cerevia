import os
from PIL import Image, ImageChops

# Path to generated logo
logo_path = r"C:\Users\Farel\.gemini\antigravity-ide\brain\33f6cd34-63aa-4c6e-9d33-82714a747085\cerevia_logo_green_1782145827648.png"

# Base mipmap directory
mipmap_base = r"C:\Users\Farel\Documents\Kuliah\Project\Cerevia\Apps\cerevia_android\app\src\main\res"

# Sizes for ic_launcher
sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

def trim(im):
    # Convert to grayscale to find bounding box of anything not pure white
    bg = Image.new(im.mode, im.size, (255, 255, 255, 255))
    diff = ImageChops.difference(im, bg)
    
    # Apply a high threshold to ignore ALL noise and get a perfect tight crop
    diff_gray = diff.convert("L")
    mask = diff_gray.point(lambda p: 255 if p > 50 else 0)
    
    bbox = mask.getbbox()
    if bbox:
        # Check if the bbox needs some balancing?
        # A mathematical center of the bounding box might not be the visual center, 
        # but the tightest crop is usually the best start.
        cropped = im.crop(bbox)
        return cropped
    return im

def make_square_with_padding(im, padding_ratio=0.0):
    w, h = im.size
    max_dim = max(w, h)
    
    target_dim = int(max_dim * (1.0 + padding_ratio * 2))
    
    new_im = Image.new("RGBA", (target_dim, target_dim), (255, 255, 255, 255))
    # EXACT integer division for centering
    offset = ((target_dim - w) // 2, (target_dim - h) // 2)
    new_im.paste(im, offset)
    return new_im

try:
    img = Image.open(logo_path)
    img = img.convert("RGBA")
    
    img = trim(img)
    
    # Use 8% padding to avoid getting cut off too much, and perfect centering
    img = make_square_with_padding(img, padding_ratio=0.08)
    
    for folder, size in sizes.items():
        out_dir = os.path.join(mipmap_base, folder)
        os.makedirs(out_dir, exist_ok=True)
        
        # Resize image
        resized = img.resize((size, size), Image.Resampling.LANCZOS)
        
        # Save as ic_launcher.png
        out_path = os.path.join(out_dir, "ic_launcher.png")
        resized.save(out_path, format="PNG")
        
        # Also save ic_launcher_round.png
        mask = Image.new('L', (size, size), 0)
        from PIL import ImageDraw
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, size, size), fill=255)
        
        round_img = resized.copy()
        round_img.putalpha(mask)
        
        round_path = os.path.join(out_dir, "ic_launcher_round.png")
        round_img.save(round_path, format="PNG")
        
        print(f"Saved {size}x{size} to {folder}")

    print("Success!")
except Exception as e:
    print(f"Error: {e}")
