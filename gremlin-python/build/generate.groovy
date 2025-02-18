/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.tinkerpop.gremlin.language.translator.GremlinTranslator
import org.apache.tinkerpop.gremlin.language.translator.Translator
import org.apache.tinkerpop.gremlin.language.corpus.FeatureReader

import java.nio.file.Paths

// file is overwritten on each generation
radishGremlinFile = new File("${projectBaseDir}/gremlin-python/src/main/python/radish/gremlin.py")

// assumes globally unique scenario names for keys with list of Gremlin traversals as they appear
gremlins = FeatureReader.parseGrouped(Paths.get("${projectBaseDir}", "gremlin-test", "src", "main", "resources", "org", "apache", "tinkerpop", "gremlin", "test", "features").toString())

radishGremlinFile.withWriter('UTF-8') { Writer writer ->
    writer.writeLine('#\n' +
            '# Licensed to the Apache Software Foundation (ASF) under one\n' +
            '# or more contributor license agreements.  See the NOTICE file\n' +
            '# distributed with this work for additional information\n' +
            '# regarding copyright ownership.  The ASF licenses this file\n' +
            '# to you under the Apache License, Version 2.0 (the\n' +
            '# "License"); you may not use this file except in compliance\n' +
            '# with the License.  You may obtain a copy of the License at\n' +
            '# \n' +
            '# http://www.apache.org/licenses/LICENSE-2.0\n' +
            '# \n' +
            '# Unless required by applicable law or agreed to in writing,\n' +
            '# software distributed under the License is distributed on an\n' +
            '# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n' +
            '# KIND, either express or implied.  See the License for the\n' +
            '# specific language governing permissions and limitations\n' +
            '# under the License.\n' +
            '#\n')

    writer.writeLine("\n\n#######################################################################################")
    writer.writeLine("## Do NOT edit this file directly - generated by build/generate.groovy")
    writer.writeLine("#######################################################################################\n\n")

    writer.writeLine(
                    'from radish import world\n' +
                    'import datetime\n' +
                    'from gremlin_python.statics import long, bigint, GremlinType\n' +
                    'from gremlin_python.process.anonymous_traversal import traversal\n' +
                    'from gremlin_python.process.strategies import *\n' +
                    'from gremlin_python.process.traversal import TraversalStrategy\n' +
                    'from gremlin_python.process.graph_traversal import __\n' +
                    'from gremlin_python.structure.graph import Graph\n' +
                    'from gremlin_python.process.traversal import Barrier, Cardinality, CardinalityValue, P, TextP, Pop, Scope, Column, Order, Direction, DT, Merge, T, Pick, Operator, IO, WithOptions\n')

    // some traversals may require a static translation if the translator can't handle them for some reason
    def staticTranslate = [:]
    // SAMPLE: g_injectXnull_nullX: "    'g_injectXnull_nullX': [(lambda g: g.inject(None,None))], ",

    writer.writeLine('world.gremlins = {')
    gremlins.each { k,v ->
        // skipping lambdas until we decide for sure that they are out in 4.x
        if (v.any { it.contains('l1')} || v.any { it.contains('l2')} || v.any { it.contains('c1')} || v.any { it.contains('c2')} || v.any { it.contains('pred1')} || v.any { it.contains('Lambda')}) {
            writer.writeLine("    '${k}': [],  # skipping as it contains a lambda")
        } else if (staticTranslate.containsKey(k)) {
            writer.writeLine(staticTranslate[k])
        } else {
            writer.write("    '")
            writer.write(k)
            writer.write("': [")
            def collected = v.collect { GremlinTranslator.translate(it, Translator.PYTHON) }
            def uniqueBindings = collected.collect { it.getParameters() }.flatten().unique()
            def gremlinItty = collected.iterator()
            while (gremlinItty.hasNext()) {
                def t = gremlinItty.next()
                writer.write("(lambda g")
                if (!uniqueBindings.isEmpty()) {
                    writer.write(", ")
                    writer.write(uniqueBindings.join("=None,"))
                    writer.write("=None")
                }
                writer.write(":")
                writer.write(t.getTranslated())
                writer.write(")")
                if (gremlinItty.hasNext()) writer.write(', ')
            }
            writer.writeLine('], ')
        }
    }
    writer.writeLine('}')
}


