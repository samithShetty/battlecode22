package focusfireplayerV2;

import battlecode.common.*;

import java.util.Map;
import java.util.Random;

public strictfp class RobotPlayer {

    static int turnCount = 0;
    private static final int maxInt = 65535;

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

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        while (true) {
            turnCount++;
            try {
                // Run each robot type
                switch (rc.getType()) {
                    case ARCHON:        runArchon(rc);      break;
                    case MINER:         runMiner(rc);       break;
                    case SOLDIER:       runSoldier(rc);     break;
                    case LABORATORY:    runLaboratory(rc);  break;
                    case WATCHTOWER:    runWatchtower(rc);  break;
                    case BUILDER:       runBuilder(rc);     break;
                    case SAGE:          runSage(rc);        break;
                }

                /*
                    0-1 shared Archon array
                 */
                if(turnCount == 1) {
                    for (int k = 0; k < 64; k++) {
                        rc.writeSharedArray(k, maxInt);
                    }
                }

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } finally {
                Clock.yield();
            }
        }
    }

    static void runArchon(RobotController rc) throws GameActionException {

        Direction dir = directions[rng.nextInt(directions.length)];
        if (turnCount < 50 || (turnCount > 500 && turnCount < 550)) {
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

    static void runSoldier(RobotController rc) throws GameActionException {

        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);

        // If archon is sensed write it to the array
        for(RobotInfo ri : enemies){
            if(ri.type == (RobotType.ARCHON)){
                rc.writeSharedArray(0,ri.location.x);
                rc.writeSharedArray(1, ri.location.y);
            }
        }

        // Move to or Attack archon if its found, if not attack nearest enemy
        if(rc.readSharedArray(0) != maxInt){
            MapLocation archonLocation = new MapLocation(rc.readSharedArray(0), rc.readSharedArray(1));
            if(rc.canAttack(archonLocation)){
                if(rc.senseRobotAtLocation(archonLocation).health <= 3){
                    // If the archon is about to die remove it from the array
                    rc.writeSharedArray(0,maxInt);
                    rc.writeSharedArray(1, maxInt);
                }
                rc.attack(archonLocation);
            }else {
                rc.move(rc.getLocation().directionTo(archonLocation));
            }
        } else if (enemies.length > 0){
            rc.attack(enemies[0].location);
        }

        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if(rc.canMove(dir)){
            rc.move(dir);
        }
    }

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

    static void runSage(RobotController rc) throws GameActionException {
        // TODO(*): Finish method
    }

}
