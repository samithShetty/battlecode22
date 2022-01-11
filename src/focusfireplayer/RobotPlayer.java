package focusfireplayer;

import battlecode.common.*;

import java.awt.*;
import java.util.Random;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;
    private static int maxInt = 65535;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(6597);

    /**
     * Array containing all the possible movement directions.
     */
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc The RobotController object. You use it to perform actions from this robot, and to get
     *           information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the RobotType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                switch (rc.getType()) {
                    case ARCHON:
                        runArchon(rc);
                        break;
                    case MINER:
                        runMiner(rc);
                        break;
                    case SOLDIER:
                        runSoldier(rc);
                        break;
                    case LABORATORY:
                        runLaboratory(rc);
                        break;
                    case WATCHTOWER:
                        runWatchtower(rc);
                        break;
                    case BUILDER:
                        runBuilder(rc);
                        break;
                    case SAGE:
                        runSage(rc);
                        break;
                }

                if(turnCount == 1) {
                    // set elem 0 to 0, 0 will be a placeholder for commander
                    rc.writeSharedArray(0, maxInt);
                    // set elem 0 to 0, 0 will be a placeholder for no enemy
                    rc.writeSharedArray(1, maxInt);
                    // set elem to 0, 0 will be a placeholder for enemy archon
                    rc.writeSharedArray(2, maxInt); // x
                    rc.writeSharedArray(3, maxInt); // y
                }

            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    /**
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runArchon(RobotController rc) throws GameActionException {
        // TODO(*): Refactor to include other RobotType's
        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (turnCount < 150) {
            // Let's try to build a miner.
            rc.setIndicatorString("Trying to build a miner");
            if (rc.canBuildRobot(RobotType.MINER, dir)) {
                rc.buildRobot(RobotType.MINER, dir);
            }
        } else {
            // Let's try to build a soldier.
            rc.setIndicatorString("Trying to build a soldier");
            if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                rc.buildRobot(RobotType.SOLDIER, dir);
            }
        }
    }

    /**
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runMiner(RobotController rc) throws GameActionException {

        // Try to mine on squares around us.
        MapLocation me = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                // Notice that the Miner's action cooldown is very low.
                // You can mine multiple times per turn!
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
                while (rc.canMineLead(mineLocation) && rc.senseLead(mineLocation) > 1) {
                    rc.mineLead(mineLocation);
                }
            }
        }

        // Scout for resources and move towards them
        int visionRadius = rc.getType().visionRadiusSquared;
        MapLocation[] nearbyLocations = rc.getAllLocationsWithinRadiusSquared(me, visionRadius);
        MapLocation oreLocation = null;

        for (MapLocation loc : nearbyLocations) {
            if (rc.senseGold(loc) > 0 || rc.senseLead(loc) > 15) {
                oreLocation = loc;
            }
        }

        if (oreLocation != null) {
            Direction toMove = me.directionTo(oreLocation);
            if (rc.canMove(toMove)) {
                rc.move(toMove);
            }
        }

        Direction dir = RobotPlayer.directions[RobotPlayer.rng.nextInt(RobotPlayer.directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }

        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);

        if(rc.readSharedArray(2) == maxInt){
            for(RobotInfo enemy : enemies){
                if(enemy.getType() == (RobotType.ARCHON)){
                    rc.setIndicatorDot(enemy.location, 0, 200, 200);
                    rc.setIndicatorString("!!!Archon found!!!");
                    rc.writeSharedArray(2, enemy.location.x);
                    rc.writeSharedArray(3, enemy.location.y);
                }
            }
        }
    }

    /**
     * Run a single turn for a Soldier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runSoldier(RobotController rc) throws GameActionException {

        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);

        if(rc.readSharedArray(2) == maxInt){
            for(RobotInfo enemy : enemies){
                if(enemy.getType() == (RobotType.ARCHON)){
                    rc.setIndicatorDot(enemy.location, 0, 200, 200);
                    rc.setIndicatorString("!!!Archon found!!!");
                    rc.writeSharedArray(2, enemy.location.x);
                    rc.writeSharedArray(3, enemy.location.y);
                }
            }
        }

        if(rc.readSharedArray(2) != maxInt){
            MapLocation archonLoc = new MapLocation(rc.readSharedArray(2), rc.readSharedArray(3));
            if(rc.canAttack(archonLoc)){
                if (rc.senseRobotAtLocation(archonLoc).health <= 3){
                    rc.writeSharedArray(2, maxInt);
                    rc.writeSharedArray(3, maxInt);
                }
                rc.attack(archonLoc);
            } else {
                rc.move(rc.getLocation().directionTo(archonLoc));
            }
        }

        if (enemies.length > 0 && rc.readSharedArray(2) == maxInt) {
            // If no enemy is in shared array put the nearest enemy in the shared array
            if(rc.readSharedArray(1) == maxInt){
                rc.writeSharedArray(1,enemies[0].ID);
            } else {
                // Enemy is about to die, set elem 0 to zero
                if (rc.canSenseRobot(rc.readSharedArray(1))) {
                    if (rc.senseRobot(rc.readSharedArray(1)).getHealth() <= 3) {
                        rc.attack(rc.senseRobot(rc.readSharedArray(0)).location);
                        rc.writeSharedArray(1, maxInt);
                    }
                }
                // Attack the shared robot if visible, if not attack nearest
                if (rc.canSenseRobot(rc.readSharedArray(1)) && rc.readSharedArray(1) != maxInt) {
                    rc.attack(rc.senseRobot(rc.readSharedArray(1)).location);
                } else {
                    MapLocation attackLocation = enemies[0].location;
                    rc.attack(attackLocation);
                }
            }
        }

        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir) && rc.readSharedArray(2) == maxInt) {
            rc.move(dir);
            System.out.println("I moved!");
        }
    }

    /**
     * Run a single turn for a Laboratory.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runLaboratory(RobotController rc) throws GameActionException {
        // TODO(*): Complete this method
        // Check if laboratory can transmute lead to gold.
        if (rc.canTransmute()) {
            rc.transmute();
        }
    }

    /**
     * Run a single turn for a Watchtower.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runWatchtower(RobotController rc) throws GameActionException {
        // Attacks enemies that too close
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
            }
        }
    }

    /**
     * Run a single turn for a Builder.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runBuilder(RobotController rc) throws GameActionException {
        Direction dir = directions[rng.nextInt(directions.length)];
        // TODO(*): Refactor to include other RobotType's
        if (rc.getTeamGoldAmount(rc.getTeam()) >= 50) {
            if (rc.canBuildRobot(RobotType.SAGE, dir)) {
                rc.buildRobot(RobotType.SAGE, dir);
            }
        }
        if (rng.nextBoolean()) {
            rc.setIndicatorString("Trying to build a laboratory");
            if (rc.canBuildRobot(RobotType.LABORATORY, dir)) {
                rc.buildRobot(RobotType.LABORATORY, dir);
            }
        } else {
            // Let's try to build a watchtower.
            rc.setIndicatorString("Trying to build a watchtower");
            if (rc.canBuildRobot(RobotType.WATCHTOWER, dir)) {
                rc.buildRobot(RobotType.WATCHTOWER, dir);
            }
        }
    }

    /**
     * Run a single turn for a Sage.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runSage(RobotController rc) throws GameActionException {
        // TODO(*): Finish method
    }

}
