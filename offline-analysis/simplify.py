infile = "/tmp/randomFileForLogs.dsa"
outfile = "/tmp/cleanedLogs.csv"

with open(infile, "r") as fhin:
    with open(outfile, "w") as fhout:
        line = fhin.readline()
        line = line.replace("&", ",")
        line = line.replace("?", ",")

        cols = list(map(lambda p: p.split("=")[0], line.split(",")))
        fhout.write(",".join(cols) + "\n")
        
        while len(line) > 0:
            vals = list(map(lambda p: p.split("=")[1], line.split(",")))
            fhout.write(",".join(vals))

            line = fhin.readline()
            line = line.replace("&", ",")
            line = line.replace("?", ",")
