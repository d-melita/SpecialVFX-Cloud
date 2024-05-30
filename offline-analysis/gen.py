import random

def gen_image_proc_script(t):
    FILE=f"{t}.json"
    reps = 10

    images = [
        "empty-200x200.png",
        "empty-200x600.png",
        "empty-200x1000.png",
        "empty-200x1400.png",
        "empty-200x1800.png",
        "empty-200x2200.png",
        "empty-200x2600.png",
        "empty-200x3000.png",
        "empty-200x3400.png",
        "empty-200x3800.png",
        "empty-600x200.png",
        "empty-600x600.png",
        "empty-600x1000.png",
        "empty-600x1400.png",
        "empty-600x1800.png",
        "empty-600x2200.png",
        "empty-600x2600.png",
        "empty-600x3000.png",
        "empty-600x3400.png",
        "empty-600x3800.png",
        "empty-1000x200.png",
        "empty-1000x600.png",
        "empty-1000x1000.png",
        "empty-1000x1400.png",
        "empty-1000x1800.png",
        "empty-1000x2200.png",
        "empty-1000x2600.png",
        "empty-1000x3000.png",
        "empty-1000x3400.png",
        "empty-1000x3800.png",
        "empty-1400x200.png",
        "empty-1400x600.png",
        "empty-1400x1000.png",
        "empty-1400x1400.png",
        "empty-1400x1800.png",
        "empty-1400x2200.png",
        "empty-1400x2600.png",
        "empty-1400x3000.png",
        "empty-1400x3400.png",
        "empty-1400x3800.png",
        "empty-1800x200.png",
        "empty-1800x600.png",
        "empty-1800x1000.png",
        "empty-1800x1400.png",
        "empty-1800x1800.png",
        "empty-1800x2200.png",
        "empty-1800x2600.png",
        "empty-1800x3000.png",
        "empty-1800x3400.png",
        "empty-1800x3800.png",
        "empty-2200x200.png",
        "empty-2200x600.png",
        "empty-2200x1000.png",
        "empty-2200x1400.png",
        "empty-2200x1800.png",
        "empty-2200x2200.png",
        "empty-2200x2600.png",
        "empty-2200x3000.png",
        "empty-2200x3400.png",
        "empty-2200x3800.png",
        "empty-2600x200.png",
        "empty-2600x600.png",
        "empty-2600x1000.png",
        "empty-2600x1400.png",
        "empty-2600x1800.png",
        "empty-2600x2200.png",
        "empty-2600x2600.png",
        "empty-2600x3000.png",
        "empty-2600x3400.png",
        "empty-2600x3800.png",
        "empty-3000x200.png",
        "empty-3000x600.png",
        "empty-3000x1000.png",
        "empty-3000x1400.png",
        "empty-3000x1800.png",
        "empty-3000x2200.png",
        "empty-3000x2600.png",
        "empty-3000x3000.png",
        "empty-3000x3400.png",
        "empty-3000x3800.png",
        "empty-3400x200.png",
        "empty-3400x600.png",
        "empty-3400x1000.png",
        "empty-3400x1400.png",
        "empty-3400x1800.png",
        "empty-3400x2200.png",
        "empty-3400x2600.png",
        "empty-3400x3000.png",
        "empty-3400x3400.png",
        "empty-3400x3800.png",
        "empty-3800x200.png",
        "empty-3800x600.png",
        "empty-3800x1000.png",
        "empty-3800x1400.png",
        "empty-3800x1800.png",
        "empty-3800x2200.png",
        "empty-3800x2600.png",
        "empty-3800x3000.png",
        "empty-3800x3400.png",
        "empty-3800x3800.png",
    ]

    def write_single(image, fh):
        fh.write(f'{{"type": "{t}", "input": "{image}", "output": "{image}.out" }}')

    images = images * reps # to spread repetitions
    with open(FILE, "w") as fh:
        fh.write("[")
        for image in images[:-1]:
            write_single(image, fh)
            fh.write(",")

        write_single(images[-1], fh)
        fh.write("]")

def gen_ray_tracer_script(light_count, shapes, txtfile, fh, wcols, wrows, texture = None):
    NPIG = 2
    NFIN = 2

    def get_random_point():
        return f"{random.uniform(-10, 10)} {random.uniform(-10, 10)} {random.uniform(-10, 10)}"

    def get_random_light():
        point = f"{random.uniform(-100, 100)} {random.uniform(-100, 100)} {random.uniform(-100, 100)}"
        color = f"{random.uniform(0, 1)} {random.uniform(0, 1)} {random.uniform(0, 1)}"
        a_b_c = f"{random.uniform(0, 1)} {random.uniform(0, 1)} {random.uniform(0, 1)}"
        return f"{point} {color} {a_b_c}\n"

    def get_random_pigment():
        return f"solid {random.uniform(0, 1)} {random.uniform(0, 1)} {random.uniform(0, 1)}\n"

    def get_random_finish():
        return f"{random.uniform(0, 1)} {random.uniform(0, 1)} {random.uniform(0, 1)} {random.randint(0, 1000)} {random.uniform(0, 1)} {random.uniform(0, 1)} {random.uniform(0, 1)}\n"

    def get_random_sphere():
        pig_id = random.randint(0, NPIG - 1)
        fin_id = random.randint(0, NFIN - 1)
        point = f"{random.uniform(-20, 20)} {random.uniform(-20, 20)} {random.uniform(-20, 20)}"
        radius = random.uniform(0.1, 2)
        return f"{pig_id} {fin_id} sphere {point} {radius}\n"

    def get_random_disc():
        pig_id = random.randint(0, NPIGT - 1)
        fin_id = random.randint(0, NFIN - 1)
        center = get_random_point()
        normal = f"{random.uniform(-1, 1)} {random.uniform(-1, 1)} {random.uniform(-1, 1)}"
        radius = random.uniform(0.1, 5)
        return f"{pig_id} {fin_id} disc {center} {normal} {radius}\n"

    def get_texture(texture):
        return f"texmap {texture} {random.uniform(0, 1)} {random.uniform(0, 1)} {random.uniform(0, 1)} {random.randint(0, 1)} {random.uniform(0, 1)} {random.uniform(0, 1)} {random.uniform(0, 1)} {random.uniform(0, 1)}"

    def gen_scene(fovy, light_count, shapes, scenefile, texture):
        with open(scenefile, "w") as fh:
            # eye point
            fh.write("0 30 -200\n")

            # center point
            fh.write("0 10 -100\n")

            # up vector
            fh.write("0 1 0\n")

            # fovy
            fh.write("40\n")

            # number of lights
            fh.write(str(light_count))
            fh.write("\n")
            for _ in range(light_count):
                fh.write(get_random_light())
            fh.write("\n")

            # pigments
            NPIGT = NPIG if texture is None else NPIG + 1
            fh.write(str(NPIGT))
            fh.write("\n")
            for i in range(NPIG):
                fh.write(get_random_pigment())
            if texture is not None:
                fh.write(get_texture(texture))
            fh.write("\n")

            # finishes
            fh.write(str(NFIN))
            fh.write("\n")
            for i in range(NFIN):
                fh.write(get_random_finish())
            fh.write("\n")

            # shapes
            fh.write(str(sum(shapes.values())))
            fh.write("\n")
            for shape in shapes:
                for _ in range(shapes[shape]):
                    if shape == "sphere":
                        fh.write(get_random_sphere())
                    elif shape == "disc":
                        fh.write(get_random_disc())
                    else:
                        raise "bad shape"

    gen_scene(30, light_count, shapes , txtfile, None if texture is None else f"textures/{texture}") 

    if texture is None:
        fh.write(f'{{ "type": "ray-tracer", "input": "{txtfile}", "output": "{txtfile}.out", "scols": 400, "srows": 300, "wcols": {wcols}, "wrows": {wrows}, "coff": 0, "roff": 0 }}')
    else:
        fh.write(f'{{ "type": "ray-tracer", "input": "{txtfile}", "output": "{txtfile}.out", "scols": 400, "srows": 300, "wcols": {wcols}, "wrows": {wrows}, "coff": 0, "roff": 0, "texture": "textures/{texture}" }}')

textures = [
    "calcada.jpeg",
    "cement.jpeg",
    "chipboard.jpeg",
    "cloth.jpeg",
    "paper.jpeg"
]

# gen_image_proc_script("enhance")
# gen_image_proc_script("blur")

FILE = "raytracer.json"
reps = 10


def try_spheres():
    sphere_counts = list(range(1, 30))
    sphere_counts = sphere_counts * reps # to spread repetitions
    with open(FILE, "w") as fh:
        fh.write("[")
        for i, sphere_count in enumerate(sphere_counts[:-1]):
            # gen_ray_tracer_script(3, {"sphere": sphere_count}, f"scene-{sphere_count}.txt", fh, texture = textures[0])
            gen_ray_tracer_script(3, {"sphere": sphere_count}, f"scene-{i}.txt", fh, 400, 300)
            fh.write(",")

        # gen_ray_tracer_script(3, {"sphere": sphere_counts[-1]}, f"scene-{len(sphere_counts)-1}.txt", fh, texture = textures[0])
        gen_ray_tracer_script(3, {"sphere": sphere_counts[-1]}, f"scene-{len(sphere_counts)-1}.txt", fh, 400, 300)
        fh.write("]")

def try_lights():
    light_counts = list(range(1, 30))
    light_counts = light_counts * reps # to spread repetitions
    with open(FILE, "w") as fh:
        fh.write("[")
        for i, light_count in enumerate(light_counts[:-1]):
            gen_ray_tracer_script(light_count, {"sphere": 5}, f"scene-{i}.txt", fh, 400, 300)
            fh.write(",")

        gen_ray_tracer_script(light_counts[-1], {"sphere": 5}, f"scene-{len(light_counts)-1}.txt", fh, 400, 300)
        fh.write("]")

def try_width():
    widths = list(range(100, 1100, 100))
    widths = widths * reps # to spread repetitions
    with open(FILE, "w") as fh:
        fh.write("[")
        for i, width in enumerate(widths[:-1]):
            gen_ray_tracer_script(3, {"sphere": 5}, f"scene-{i}.txt", fh, width, 300)
            fh.write(",")

        gen_ray_tracer_script(3, {"sphere": 5}, f"scene-{len(widths)-1}.txt", fh, widths[-1], 300)
        fh.write("]")

def try_height():
    heights = list(range(100, 1100, 100))
    heights = heights * reps # to spread repetitions
    with open(FILE, "w") as fh:
        fh.write("[")
        for i, height in enumerate(heights[:-1]):
            gen_ray_tracer_script(3, {"sphere": 5}, f"scene-{i}.txt", fh, 400, height)
            fh.write(",")

        gen_ray_tracer_script(3, {"sphere": 5}, f"scene-{len(heights)-1}.txt", fh, 400, heights[-1])
        fh.write("]")

def try_shape():
    pass

gen_image_proc_script("blur")
# try_height()
