package dk.alexandra.benchmarking;

import dk.alexandra.fresco.framework.network.Network;

public interface Program {

    default String name() {
        return "UNNAMED PROGRAM";
    }

    void ready(Network network, int myId);

    void run();
}
