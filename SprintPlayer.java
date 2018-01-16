//import the API.
//See xxx for the javadocs.
import java.util.ArrayList;

import bc.*;

public class SprintPlayer {
	public static Direction[] directions;
	public static boolean finishedBuilding;
	public static MapLocation swarmLoc;
	public static Team myTeam;
	public static Team opponentTeam;
	
	public static void main(String[] args) {

     // One slightly weird thing: some methods are currently static methods on a static class called bc.
     // This will eventually be fixed :/
     System.out.println("Opposite of " + Direction.North + ": " + bc.bcDirectionOpposite(Direction.North));

     // Connect to the manager, starting the game
     GameController gc = new GameController();

     // Direction is a normal java enum.
     directions = Direction.values();
     MapLocation buildLoc = null;
     myTeam = gc.team();
     if (myTeam.equals(Team.Blue)) {
    	 	opponentTeam = Team.Red;
     } else {
    	 	opponentTeam = Team.Blue;
     }
     
     // research code
     gc.queueResearch(UnitType.Worker);
     gc.queueResearch(UnitType.Rocket);
     gc.queueResearch(UnitType.Knight);
     gc.queueResearch(UnitType.Knight);
     gc.queueResearch(UnitType.Worker);
     gc.queueResearch(UnitType.Worker);
     gc.queueResearch(UnitType.Rocket);
     gc.queueResearch(UnitType.Worker);

     while (true) {
    	 	 int roundNum = (int) gc.round();
    	 	 System.out.println(gc.researchInfo().getLevel(UnitType.Knight));
         System.out.println("Current round: "+roundNum);
         // VecUnit is a class that you can think of as similar to ArrayList<Unit>, but immutable.
         VecUnit units = gc.myUnits();
         // set initial base
         if (buildLoc == null && units.size() != 0) {
        	 	buildLoc = gc.myUnits().get(0).location().mapLocation().add(Direction.South);
         }
         ArrayList<Unit> workers = new ArrayList<Unit>();
         ArrayList<Unit> factories = new ArrayList<Unit>();
         ArrayList<Unit> knights = new ArrayList<Unit>();
         Planet currentPlanet = Planet.Earth;
         for (int i = 0; i < units.size(); i++) {
             Unit unit = units.get(i);
             
     	 	
             switch (unit.unitType()) {
             case Factory:
            	 		factories.add(unit);
             		break;
             case Healer:
             		runHealer();
             		break;
             case Knight:
             		knights.add(unit);
             		break;
             case Mage:
             		runMage();
             		break;
             case Ranger:
             		runRanger();
             		break;
             case Rocket:
             		runRocket();
             		break;
             case Worker:
            	 		MapLocation myLoc = unit.location().mapLocation();
            	 		int visionRange = (int) unit.visionRange();
            	 		VecUnit enemies = gc.senseNearbyUnitsByTeam(myLoc, visionRange, opponentTeam);
            	 		if (enemies.size() > 0) {
            	 			Unit enemy = findClosestEnemy(gc, unit, enemies);
                	 		if (swarmLoc == null) {
                	 			swarmLoc = enemy.location().mapLocation();
                	 		}
            	 		}
            	 		workers.add(unit);
             		break;
             }

             // Most methods on gc take unit IDs, instead of the unit objects themselves.
//             Direction random = directions[(int) (Math.random() * 8 + 1)];
//             if (gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), random)) {
//                 gc.moveRobot(unit.id(), random);
//             }
         }
         
         // worker code
         int buildTeamSize = 4;
         // loop through workers
         for (int i = 0; i < workers.size(); i++) {
 	 		Unit worker = workers.get(i);
 	 		
 	 	
        	 	if (workers.size() >= buildTeamSize && i < buildTeamSize && (factories.size() < 3 || !finishedBuilding)) {
        	 		switch (i) {
        	 		case 0:
        	 			runBuildSequence(gc, worker, buildLoc, UnitType.Factory, factories.size());
        	 		case 1:
        	 			runBuildSequence(gc, worker, buildLoc, UnitType.Factory, factories.size());
        	 		case 2:
        	 			runBuildSequence(gc, worker, buildLoc, UnitType.Factory, factories.size());
        	 		case 3:
        	 			runBuildSequence(gc, worker, buildLoc, UnitType.Factory, factories.size());
        	 		}
        	 	} else {
        	 		runIdle(gc, worker);
        	 		if (workers.size() < 4) {
        	 			produceWorkers(gc, worker);
        	 		}
        	 		if (workers.size() < 10 && factories.size() == 3) {
        	 			produceWorkers(gc, worker);
        	 		}
        	 	}
         }
         
         // knight code
         for (int i = 0; i < knights.size(); i++) {
        	 	Unit knight = knights.get(i);
        	 	if (!knight.location().isInGarrison() && !knight.location().isInSpace()) {
        	 		MapLocation myLoc = knight.location().mapLocation();
            	 	int visionRange = (int) knight.visionRange();
            	 	VecUnit enemies = gc.senseNearbyUnitsByTeam(myLoc, visionRange, opponentTeam);
            	 	MapLocation attackLoc = swarmLoc;
            	 	
            	 	// if reached target, stop swarming
        	 		if (swarmLoc != null && myLoc.equals(swarmLoc)) {
        	 			swarmLoc = null;
        	 		}
        	 		
            	 	if (enemies.size() > 0) {
            	 		// find closest enemy
            	 		Unit closestEnemy = findClosestEnemy(gc, knight, enemies);
            	 		long dist = myLoc.distanceSquaredTo(closestEnemy.location().mapLocation());
            	 		
            	 		// attack closest enemy
            	 		attackLoc = closestEnemy.location().mapLocation();
            	 		if (dist == 1 && gc.isAttackReady(knight.id()) && gc.canAttack(knight.id(), closestEnemy.id())) {
            	 			gc.attack(knight.id(), closestEnemy.id());
            	 		}
            	 		
            	 		if (swarmLoc == null) {
            	 			swarmLoc = attackLoc;
            	 		}
            	 	}
            	 	if (attackLoc == null) {
            	 		runIdle(gc, knight);
            	 	} else {
            	 		moveToLoc(gc, knight, attackLoc);
            	 	}
        	 	}
         }
         
         // factory code
         runFactories(gc, factories);
         
         // Submit the actions we've done, and wait for our next turn.
         gc.nextTurn();
     }
 }
	
	private static Unit findClosestEnemy(GameController gc, Unit unit, VecUnit enemies) {
		Unit closestEnemy = enemies.get(0);
		MapLocation myLoc = unit.location().mapLocation();
 		long shortestDist = myLoc.distanceSquaredTo(closestEnemy.location().mapLocation());
 		for (int j = 1; j < enemies.size(); j++) {
 			Unit enemy = enemies.get(j);
 			long dist = myLoc.distanceSquaredTo(enemy.location().mapLocation());
 			if (dist < shortestDist) {
 				shortestDist = dist;
 				closestEnemy = enemy;
 			}
 		}
 		return closestEnemy;
	}

	private static void runBuildSequence(GameController gc, Unit worker, MapLocation buildLoc, UnitType buildType, int factoryNum) {
 		MapLocation myLoc = worker.location().mapLocation();
 		if (myLoc.isAdjacentTo(buildLoc)) {
 			Direction buildDir = myLoc.directionTo(buildLoc);
 			// if no blueprint, then put one there
 			// else if there is a blueprint, work on blueprint
 			if (gc.canBlueprint(worker.id(), buildType, buildDir)) {
 				gc.blueprint(worker.id(), buildType, buildDir);
 				finishedBuilding = false;
 			} else if (gc.hasUnitAtLocation(buildLoc)){
 				Unit blueprint = gc.senseUnitAtLocation(buildLoc);
 				// if can build blueprint, then do so
 				// if done, then move on to next factory
 				if (gc.canBuild(worker.id(), blueprint.id())) {
 					gc.build(worker.id(), blueprint.id());
 				}
 				if (blueprint.health() == blueprint.maxHealth()) {
 					System.out.println("Move to next factory");
 					MapLocation possibleLoc = findOpenAdjacentSpot(gc, myLoc);
 					if (possibleLoc != null) {
 						buildLoc.setX(possibleLoc.getX());
 						buildLoc.setY(possibleLoc.getY());
 					}
 					if (factoryNum == 3) {
 						finishedBuilding = true;
 					}
 				}
 			}
 		} else {
 			// move towards build location
 	 		moveToLoc(gc, worker, buildLoc);
 		}
	}

	private static MapLocation findOpenAdjacentSpot(GameController gc, MapLocation myLoc) {
		MapLocation returnLoc = null;
		boolean dirFound = false;
		int counter = 0;
		while (!dirFound && counter < 8) {
			MapLocation possibleLoc = myLoc.add(directions[counter]);
			if (gc.isOccupiable(possibleLoc) == 1) {
				dirFound = true;
				returnLoc = possibleLoc;
			}
			counter++;
		}
		return returnLoc;
	}

	private static void moveToLoc(GameController gc, Unit unit, MapLocation targetLoc) {
 		// get current location and target direction
		MapLocation myLoc = unit.location().mapLocation();
 		Direction targetDir = myLoc.directionTo(targetLoc);
 		
 		// if unit can move to target location then it does
 		if (gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), targetDir)) {
 			gc.moveRobot(unit.id(), targetDir);
 		}
	}

	private static void runIdle(GameController gc, Unit unit) {
		Planet planet = gc.planet();
 		MapLocation myLoc = unit.location().mapLocation();
 		Direction random = directions[(int) (Math.random() * 8 + 1)];
 		//move randomly
 		moveToLoc(gc, unit, myLoc.add(random));
	}


	private static void produceWorkers(GameController gc, Unit worker) {
		Planet planet = gc.planet();
 		Direction random = directions[(int) (Math.random() * 8 + 1)];
 		//replicates worker in random direction
 		if(gc.karbonite() >= 15 && gc.canReplicate(worker.id(), random)) {
 			gc.replicate(worker.id(), random);
 		}
	}

	private static void runRocket() {
		// TODO Auto-generated method stub
		
	}

	private static void runRanger() {
		// TODO Auto-generated method stub
		
	}

	private static void runMage() {
		// TODO Auto-generated method stub
		
	}

	private static void runKnight(GameController gc, Unit knight) {
		if (!knight.location().isInGarrison()) {
			runIdle(gc, knight);
		}
	}

	private static void runHealer() {
		// TODO Auto-generated method stub
		
	}

	private static void runFactories(GameController gc, ArrayList<Unit> factories) {
		Direction random = directions[(int) (Math.random() * 8 + 1)];
		if (factories.size() == 3) {
			for (Unit factory : factories) {
				if (gc.canProduceRobot(factory.id(), UnitType.Knight)) {
					gc.produceRobot(factory.id(), UnitType.Knight);
				}
				if (gc.canUnload(factory.id(), random)) {
					gc.unload(factory.id(), random);
				}
			}
		}
	}
}
