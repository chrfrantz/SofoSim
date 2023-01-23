# SofoSim
SofoSim is a simulation environment that builds on MASON (17) with the primary purpose of dealing with simulation output, alongside further helper features.

# Features

 * Builds on MASON (17) as scheduler
 * Provides statistical output feature; including parameterization of frequency, structured data, charts (based on JFreeChart and Batik), as well as network visualizations (based on JUNG).
 * Manages random number generation to ensure deterministic execution
 * Integrates Micro-agents environment for advanced organizational structure
 * Provides diverse variants of agent memory implementations
 * Integrates IT2FS for visualization of fuzzy sets
 * Includes Windows positioning utility
 * various other minor features
 
# Dependencies

## Third-party libraries


* [Micro-agents 1.0.0](https://github.com/micro-agents/micro-agents)
* [jfreechart-1.5.0.jar](https://github.com/jfree/jfreechart)
* jcommon-1.0.23.jar (part of JFreeChart)
* [Batik 1.7](https://xmlgraphics.apache.org/batik/)
* [JUNG 2.0.1](https://github.com/jrtom/jung)
* Java3D 1.5.1
* IT2FLS
* various others (see libs folder)

All dependencies are provided in the libs folder

## Build system

The environment has been developed on Java 1.8, and is best executed using OpenJDK 8.

* OpenJDK 8 ([Recommended download](https://adoptium.net/temurin/releases/?version=8))
