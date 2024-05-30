from PIL import Image
import numpy
import random

def gen(n, m):
    imarray = numpy.random.rand(n,m,3) * 255
    im = Image.fromarray(imarray.astype('uint8')).convert('RGBA')
    im.save(f"images/empty-{n}x{m}.png", "PNG")

for n in range(200, 4000, 400):
    for m in range(200, 4000, 400):
        gen(n, m)
