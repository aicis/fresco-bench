package dk.alexandra.benchmarking;

import dk.alexandra.fresco.framework.network.Network;

public interface Program {

    /**
     * Returns the name of the program
     * @return name of the program
     */
    default String name() {
        return "UNNAMED PROGRAM";
    }

    /**
     * Ready the program for benchmarking, running setups etc.
     * @param network Network of all the other parties
     * @param myId This program's own ID
     */
    void ready(Network network, int myId);

    /**
     * Start running the program
     */
    void run();
}
