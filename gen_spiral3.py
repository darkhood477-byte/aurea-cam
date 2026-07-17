import math

phi = (1 + math.sqrt(5)) / 2

# We map [0, phi] -> [0, width]
# and [0, 1] -> [0, height]
# We'll output Kotlin code

cx, cy = 1.0, 0.0 # center of first arc
radius = 1.0
start_angle = 180 # left side of the circle: (cx - radius, cy) => (0, 0)
sweep = -90 # to bottom: (cx, cy + radius) => (1, 1)

print("val path = Path()")
print("path.moveTo(0f, 0f)")

for i in range(10):
    left = cx - radius
    top = cy - radius
    right = cx + radius
    bottom = cy + radius
    
    print(f"path.arcTo(")
    print(f"    rect = androidx.compose.ui.geometry.Rect(")
    print(f"        left = width * {left/phi:.6f}f,")
    print(f"        top = height * {top:.6f}f,")
    print(f"        right = width * {right/phi:.6f}f,")
    print(f"        bottom = height * {bottom:.6f}f")
    print(f"    ),")
    print(f"    startAngleDegrees = {start_angle}f,")
    print(f"    sweepAngleDegrees = {sweep}f,")
    print(f"    forceMoveTo = false")
    print(f")")
    
    radius = radius / phi
    end_angle = (start_angle + sweep) % 360
    end_angle_rad = math.radians(end_angle)
    cx = cx + (radius * phi - radius) * math.cos(end_angle_rad)
    cy = cy + (radius * phi - radius) * math.sin(end_angle_rad)
    start_angle = end_angle

