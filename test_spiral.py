import math

phi = (1 + math.sqrt(5)) / 2
# Let the rectangle be from x=0 to phi, y=0 to 1
# Squares are formed by removing 1x1, then 1/phi x 1/phi, etc.
# Actually, let's just generate the arcs.
# Start with rect = (0, 0, phi, 1)

rects = []
x, y, w, h = 0, 0, phi, 1
direction = 0 # 0: right, 1: down, 2: left, 3: up

# We want the spiral to start large.
# If direction=0, we cut a w x w square on the right? No, cut a square from the left or right?
# Let's write the code for the arcs.
print(phi)
