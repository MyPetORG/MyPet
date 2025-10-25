rootProject.name = "MyPet"

include("api", "plugin", "skills")

val nmsModules = File(rootDir, "nms").listFiles()!!.filter { s -> s.isDirectory() && s.name.matches("v[\\d_]+R\\d+".toRegex()) }.map { s -> ":nms:" + s.name }
gradle.extra["nmsModules"] = nmsModules
include(nmsModules)
