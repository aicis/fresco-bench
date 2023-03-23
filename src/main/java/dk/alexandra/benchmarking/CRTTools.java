package dk.alexandra.benchmarking;

import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.MersennePrimeFieldDefinition;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedProtocolEvaluator;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedStrategy;
import dk.alexandra.fresco.framework.util.*;
import dk.alexandra.fresco.lib.field.integer.BasicNumericContext;
import dk.alexandra.fresco.suite.crt.CRTProtocolSuite;
import dk.alexandra.fresco.suite.crt.datatypes.resource.CRTDataSupplier;
import dk.alexandra.fresco.suite.crt.datatypes.resource.CRTDummyDataSupplier;
import dk.alexandra.fresco.suite.crt.datatypes.resource.CRTResourcePoolImpl;
import dk.alexandra.fresco.suite.spdz.SpdzBuilder;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePoolImpl;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzSInt;
import dk.alexandra.fresco.suite.spdz.storage.SpdzDataSupplier;
import dk.alexandra.fresco.tools.ot.base.DummyOt;
import dk.alexandra.fresco.tools.ot.base.Ot;
import dk.alexandra.fresco.tools.ot.otextension.RotList;

import java.math.BigInteger;
import java.util.*;

import static dk.alexandra.fresco.suite.crt.Util.randomBigInteger;

/**
 * Somebody didn't make it easy to create instances of the CRT stuff.
 * Here is some copy-paste of the test classes from Fresco.
 */
public class CRTTools {

    private static final int PRG_SEED_LENGTH = 256;

    protected static final FieldDefinition DEFAULT_FIELD_LEFT =
            MersennePrimeFieldDefinition.find(64);

    protected static final FieldDefinition DEFAULT_FIELD_RIGHT = new BigIntegerFieldDefinition(
            new BigInteger("6277101735386680703605810478201558570393289253487848005721")); //152 + 40, new Random(1234)).nextProbablePrime());

    public SecureComputationEngine<?,?> engine(int myId, int noOfParties) {
        CRTProtocolSuite<SpdzResourcePool, SpdzResourcePool> suite =
                new CRTProtocolSuite<>(
                        new SpdzBuilder(new BasicNumericContext(DEFAULT_FIELD_LEFT.getBitLength() - 24,
                                myId, noOfParties, DEFAULT_FIELD_LEFT, 16, 40)),
                        new SpdzBuilder(new BasicNumericContext(DEFAULT_FIELD_RIGHT.getBitLength() - 40,
                                myId, noOfParties, DEFAULT_FIELD_RIGHT, 16, 40)));

        return new SecureComputationEngineImpl<>(
                suite,
                new BatchedProtocolEvaluator<>(new BatchedStrategy<>(), suite)
        );
    }

    public CRTResourcePoolImpl<?,?> resourcePool(int myId, int noOfParties) {


        CRTDataSupplier<?,?> dataSupplier = new CRTDummyDataSupplier<>(myId, noOfParties,
                DEFAULT_FIELD_LEFT, DEFAULT_FIELD_RIGHT,
                x -> toSpdzSInt(x, myId, noOfParties, DEFAULT_FIELD_LEFT, new Random(1234),
                        new BigInteger(DEFAULT_FIELD_LEFT.getModulus().bitLength(), new Random(0))
                                .mod(DEFAULT_FIELD_LEFT.getModulus())),
                x -> toSpdzSInt(x, myId, noOfParties, DEFAULT_FIELD_RIGHT, new Random(1234),
                        new BigInteger(DEFAULT_FIELD_RIGHT.getModulus().bitLength(), new Random(0))
                                .mod(DEFAULT_FIELD_RIGHT.getModulus()))
        );


        // TODO: Find out where to get suppliers from
        Pair<SpdzResourcePool, SpdzResourcePool> pools = createResourcePools(myId, noOfParties, null, null);

        return new CRTResourcePoolImpl<>(myId, noOfParties, dataSupplier, pools.getFirst(), pools.getSecond());

    }

    private Pair<SpdzResourcePool, SpdzResourcePool> createResourcePools(int myId,
                                                                         int numberOfParties,
                                                                         SpdzDataSupplier supplierLeft,
                                                                         SpdzDataSupplier supplierRight
                                                                         ) {


        SpdzResourcePool rpLeft = new SpdzResourcePoolImpl(myId, numberOfParties,
                new OpenedValueStoreImpl<>(), supplierLeft, AesCtrDrbg::new);
        SpdzResourcePool rpRight = new SpdzResourcePoolImpl(myId, numberOfParties,
                new OpenedValueStoreImpl<>(), supplierRight, AesCtrDrbg::new);

        return new Pair<>(rpLeft, rpRight);
    }





    private SpdzSInt toSpdzSInt(BigInteger x, int myId, int players, FieldDefinition field,
                                Random random, BigInteger secretSharedKey) {
        List<BigInteger> shares = new ArrayList<>();
        List<BigInteger> macShares = new ArrayList<>();
        BigInteger s = BigInteger.ZERO;
        BigInteger m = BigInteger.ZERO;
        for (int i = 1; i <= players - 1; i++) {
            BigInteger share = randomBigInteger(random, field.getModulus());
            s = s.add(share).mod(field.getModulus());
            shares.add(share);

            BigInteger macShare = randomBigInteger(random, field.getModulus());
            m = m.add(macShare).mod(field.getModulus());
            macShares.add(macShare);
        }
        BigInteger share = x.subtract(s).mod(field.getModulus());
        shares.add(share);

        BigInteger macShare = x.multiply(secretSharedKey).subtract(m).mod(field.getModulus());
        macShares.add(macShare);

        return new SpdzSInt(
                field.createElement(shares.get(myId - 1)),
                field.createElement(macShares.get(myId - 1))
        );
    }


//    private SpdzDataSupplier getSupplier(int myId,
//                                         int numberOfParties,
//                                         PreprocessingStrategy preProStrat,
//                                         NetManager otGenerator,
//                                         NetManager tripleGenerator,
//                                         NetManager expPipeGenerator, FieldDefinition definition) {
//
//        SpdzDataSupplier supplier;
//        if (preProStrat == DUMMY) {
//            supplier = new SpdzDummyDataSupplier(myId, numberOfParties,
//                    definition,
//                    new BigInteger(definition.getModulus().bitLength(), new Random(0))
//                            .mod(definition.getModulus()));
//        } else if (preProStrat == MASCOT) {
//            List<Integer> partyIds =
//                    IntStream.range(1, numberOfParties + 1).boxed().collect(Collectors.toList());
//            Drbg drbg = getDrbg(myId);
//            Map<Integer, RotList> seedOts =
//                    getSeedOts(myId, partyIds, drbg, otGenerator.createExtraNetwork(myId));
//            FieldElement ssk = SpdzMascotDataSupplier.createRandomSsk(definition, PRG_SEED_LENGTH);
//            supplier = SpdzMascotDataSupplier.createSimpleSupplier(myId, numberOfParties,
//                    () -> tripleGenerator.createExtraNetwork(myId), definition.getModulus().bitLength(),
//                    definition,
//                    new Function<Integer, SpdzSInt[]>() {
//
//                        private SpdzMascotDataSupplier tripleSupplier;
//                        private CloseableNetwork pipeNetwork;
//
//                        @Override
//                        public SpdzSInt[] apply(Integer pipeLength) {
//                            if (pipeNetwork == null) {
//                                pipeNetwork = expPipeGenerator.createExtraNetwork(myId);
//                                tripleSupplier = SpdzMascotDataSupplier.createSimpleSupplier(myId, numberOfParties,
//                                        () -> pipeNetwork, definition.getModulus().bitLength(), definition, null,
//                                        seedOts, drbg, ssk);
//                            }
//                            DRes<List<DRes<SInt>>> pipe =
//                                    createPipe(myId, numberOfParties, pipeLength, pipeNetwork, tripleSupplier,
//                                            definition.getBitLength());
//                            return computeSInts(pipe);
//                        }
//                    }, seedOts, drbg, ssk);
//        } else {
//            // case STATIC:
//            int noOfThreadsUsed = 1;
//            String storageName = SpdzStorageDataSupplier.STORAGE_NAME_PREFIX + noOfThreadsUsed + "_"
//                    + myId + "_" + 0 + "_";
//            FilebasedStreamedStorageImpl storage =
//                    new FilebasedStreamedStorageImpl(new InMemoryStorage());
//            supplier = new SpdzStorageDataSupplier(storage, storageName, numberOfParties);
//        }
//        return supplier;
//    }


    private static Drbg getDrbg(int myId) {
        byte[] seed = new byte[PRG_SEED_LENGTH / 8];
        new Random(myId).nextBytes(seed);
        return AesCtrDrbgFactory.fromDerivedSeed(seed);
    }

    private static Map<Integer, RotList> getSeedOts(int myId, List<Integer> partyIds,
                                                    Drbg drbg, Network network) {
        Map<Integer, RotList> seedOts = new HashMap<>();
        for (Integer otherId : partyIds) {
            if (myId != otherId) {
                Ot ot = new DummyOt(otherId, network);
                RotList currentSeedOts = new RotList(drbg, PRG_SEED_LENGTH);
                if (myId < otherId) {
                    currentSeedOts.send(ot);
                    currentSeedOts.receive(ot);
                } else {
                    currentSeedOts.receive(ot);
                    currentSeedOts.send(ot);
                }
                seedOts.put(otherId, currentSeedOts);
            }
        }
        return seedOts;
    }
}
