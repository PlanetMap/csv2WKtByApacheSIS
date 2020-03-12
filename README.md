# CSV to WKT with Apache SIS

Parse Coordinate Reference System (CRS) definitions of astronomical bodies
(planets, satellite, asteroids, …) from comma-separated values (CSV) files,
and rewrite those definitions in ISO 19162 Well Known Text (WKT) format.
The Apache Spatial Information System (SIS) library is used for this process.
A limitation is that only the type of CRS supported by Apache SIS can be converted.

This is experimental code only, not for production use.
