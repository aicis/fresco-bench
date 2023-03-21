package dk.alexandra.benchmarking;

import dk.alexandra.fresco.demo.DistanceDemo;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedProtocolEvaluator;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedStrategy;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.util.AesCtrDrbg;
import dk.alexandra.fresco.framework.util.ModulusFinder;
import dk.alexandra.fresco.suite.spdz.SpdzProtocolSuite;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePoolImpl;
import dk.alexandra.fresco.suite.spdz.storage.SpdzDummyDataSupplier;
import dk.alexandra.fresco.suite.spdz.storage.SpdzOpenedValueStoreImpl;

import java.math.BigInteger;

public class ExampleProgram implements Program {

    private Network network;
    private DistanceDemo app;
    private ResourcePool resourcePool;
    private SecureComputationEngine<ResourcePool, ProtocolBuilderNumeric> sce;

    public String name() {
        return "Example Program";
    }

    public void ready(Network network, int myId) {
        this.network = network;
        SpdzProtocolSuite suite = new SpdzProtocolSuite(80);
        sce = new SecureComputationEngineImpl(
                suite,
                new BatchedProtocolEvaluator<>(new BatchedStrategy<>(), suite)
        );

        BigInteger modulus = ModulusFinder.findSuitableModulus(512);
        resourcePool = new SpdzResourcePoolImpl(myId, network.getNoOfParties(), new SpdzOpenedValueStoreImpl(),
                new SpdzDummyDataSupplier(myId, network.getNoOfParties(),
                        new BigIntegerFieldDefinition(modulus), modulus),
                AesCtrDrbg::new);

        app = new DistanceDemo(myId, 2, 3);


    }

    public void run() {
        sce.runApplication(app, resourcePool, network);
    }
}
