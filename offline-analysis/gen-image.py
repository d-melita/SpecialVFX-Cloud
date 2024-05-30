from PIL import Image
import numpy
import random

def gen(n, m):
    imarray = numpy.random.rand(n,m,3) * 255
    im = Image.fromarray(imarray.astype('uint8')).convert('RGBA')
    im.save(f"images/empty-{n}x{m}.png", "PNG")

for i in range(50):
    print(f"Generating image {i}")
    n = random.randrange(1, 5000)
    m = random.randrange(1, 5000)
    gen(n, m)
