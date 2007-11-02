# This file is part of the RealityGrid Steering Library Java Wrappers.
# 
# (C) Copyright 2007, University of Manchester, United Kingdom,
# all rights reserved.
# 
# This software was developed by the RealityGrid project
# (http://www.realitygrid.org), funded by the EPSRC under grants
# GR/R67699/01 and GR/R67699/02.
# 
# LICENCE TERMS
# 
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
# 1. Redistributions of source code must retain the above copyright
#    notice, this list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in the
#    documentation and/or other materials provided with the distribution.
# 
# THIS MATERIAL IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. THE ENTIRE RISK AS TO THE QUALITY
# AND PERFORMANCE OF THE PROGRAM IS WITH YOU.  SHOULD THE PROGRAM PROVE
# DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR
# CORRECTION.
# 
# Author........: Robert Haines

LIBPATH=${REG_STEER_HOME}/wrappers/java

jar: classes
	jar cfm SwingSteerer.jar src/Manifest.txt -C build . -C src resources
	rm -rf build

classes:
	-mkdir build
	javac -cp ${LIBPATH} -sourcepath src -d build src/*.java

distclean: clean
	find . -name \*~ -exec rm -f {} \;
	rm -rf build
	rm -f lib

clean:
	rm -f SwingSteerer.jar
	rm -f src/*.class
