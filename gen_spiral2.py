import math

phi = (1 + math.sqrt(5)) / 2

# We will trace the path
x, y = 0.0, 1.0 # starting point
cx, cy = 1.0, 1.0 # center of first arc
radius = 1.0
start_angle = 180 # from (0,1) with center (1,1) is left, which is 180 degrees
sweep = -90

print("val path = Path()")
print("path.moveTo(0f, height)")

# We map [0, phi] -> [0, width]
# and [0, 1] -> [0, height]
# We'll output Kotlin code

for i in range(10):
    # bounding box for the arc is (cx - radius, cy - radius, cx + radius, cy + radius)
    left = cx - radius
    top = cy - radius
    right = cx + radius
    bottom = cy + radius
    
    # We want to output arcTo. 
    # arcTo(rect, startAngle, sweepAngle, forceMoveTo)
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
    
    # prepare for next
    # next radius
    radius = radius / phi
    
    # The new center moves from the old center by (old_radius - new_radius) in the direction of the end of the current arc.
    # The end of the current arc is at start_angle + sweep.
    end_angle = (start_angle + sweep) % 360
    # The new center is in the direction of end_angle from the old center?
    # No, the point on the arc is shared.
    # The shared point is P = (cx + r*cos(end), cy + r*sin(end)).
    # The new center cx2, cy2 must have P at its start_angle2.
    # The new start angle is end_angle.
    # So P = (cx2 + r2*cos(end), cy2 + r2*sin(end)).
    # Therefore, cx2 = cx + (r - r2)*cos(end)
    # cy2 = cy + (r - r2)*sin(end)
    
    end_angle_rad = math.radians(end_angle)
    cx = cx + (radius * phi - radius) * math.cos(end_angle_rad)
    cy = cy + (radius * phi - radius) * math.sin(end_angle_rad)
    
    start_angle = end_angle

