package com.tinkerpop.gremlin.hadoop.process.graph.strategy;

import com.tinkerpop.gremlin.hadoop.structure.HadoopElement;
import com.tinkerpop.gremlin.process.Step;
import com.tinkerpop.gremlin.process.T;
import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.TraversalEngine;
import com.tinkerpop.gremlin.process.TraversalStrategy;
import com.tinkerpop.gremlin.process.graph.step.filter.HasStep;
import com.tinkerpop.gremlin.process.graph.step.sideEffect.GraphStep;
import com.tinkerpop.gremlin.process.graph.step.sideEffect.IdentityStep;
import com.tinkerpop.gremlin.process.graph.step.sideEffect.StartStep;
import com.tinkerpop.gremlin.process.graph.strategy.AbstractTraversalStrategy;
import com.tinkerpop.gremlin.process.graph.strategy.TraverserSourceStrategy;
import com.tinkerpop.gremlin.process.util.TraversalHelper;
import com.tinkerpop.gremlin.structure.Compare;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.util.EmptyGraph;
import com.tinkerpop.gremlin.structure.util.HasContainer;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class HadoopElementStepStrategy extends AbstractTraversalStrategy {

    private static final HadoopElementStepStrategy INSTANCE = new HadoopElementStepStrategy();
    private final static Set<Class<? extends TraversalStrategy>> POSTS = Stream.of(TraverserSourceStrategy.class).collect(Collectors.toSet());

    private HadoopElementStepStrategy() {
    }

    @Override
    public void apply(final Traversal<?, ?> traversal, final TraversalEngine engine) {
        if (engine.equals(TraversalEngine.STANDARD))
            return;

        final StartStep<Element> startStep = (StartStep) TraversalHelper.getStart(traversal);
        if (startStep.startAssignableTo(Vertex.class, Edge.class)) {
            final HadoopElement element = ((StartStep<?>) startStep).getStart();
            final String label = TraversalHelper.getStart(traversal).getLabel();
            TraversalHelper.removeStep(TraversalHelper.getStart(traversal), traversal);
            if (TraversalHelper.isLabeled(label)) {
                final Step identityStep = new IdentityStep(traversal);
                identityStep.setLabel(label);
                TraversalHelper.insertStep(identityStep, 0, traversal);
            }
            TraversalHelper.insertStep(new HasStep(traversal, new HasContainer(T.id, Compare.eq, element.id())), 0, traversal);
            TraversalHelper.insertStep(new GraphStep<>(traversal, EmptyGraph.instance(), element.getClass()), 0, traversal);
        }
    }

    public Set<Class<? extends TraversalStrategy>> applyPost() {
        return POSTS;
    }

    public static HadoopElementStepStrategy instance() {
        return INSTANCE;
    }
}
