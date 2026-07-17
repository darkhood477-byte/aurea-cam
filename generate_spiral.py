import math

phi = (1 + math.sqrt(5)) / 2

# We will generate the arcs.
# Let's start with a bounding box [0, 0, phi, 1].
# Square 1: right side. x in [phi-1, phi], y in [0, 1].
# Actually, the standard golden spiral starts from the outside.
# Let's keep a list of arcs. Each arc is a quarter circle.
# An arc is defined by its bounding box (left, top, right, bottom) and startAngle, sweepAngle.

arcs = []
x, y, w, h = 0, 0, phi, 1

# 0: left, 1: bottom, 2: right, 3: top
# Wait, if w > h, we cut a square h x h.
# To make it a standard spiral (e.g. curling into the center):
# Let's say it starts from the left edge, moving right and down.
# Rect: (0, 0, phi, 1)
# 1. Square: (0, 0, 1, 1). Arc center: (1, 0)? No, arc from (0,1) to (1,0). Center is (1,1).
# So arc bounding box: center is (1,1), radius is 1. Bounding box: (0, 0, 2, 2). 
# Start angle: 180 (left), sweep: -90 (to top).
# Remaining rect: (1, 0, phi, 1). w=phi-1, h=1.
# 2. Square: (1, 0, phi, phi-1). Center is (1, phi-1). Radius=phi-1.
# And so on.

# Let's do 10 steps.
x, y = 0.0, 0.0
w, h = phi, 1.0
direction = 0 # 0: cut left, 1: cut top, 2: cut right, 3: cut bottom
# Actually, we always cut the largest square.
# Let's use a simpler approach:
# The theoretical spiral can be hardcoded.

print("val path = Path()")

pts = []
# logarithmic spiral r = a * e^(b * theta)
b = math.log(phi) / (math.pi / 2)
# We need to find the pole and a.
# Let's just generate the quarter circles.
