import bc.*;
public class EarthDeposit{
	private MapLocation loc;
	private long count;
	
	public EarthDeposit(MapLocation boop, PlanetMap earthMap){
		loc = boop;
		count = earthMap.initialKarboniteAt(boop);
	}
	
	public EarthDeposit(MapLocation boop, long num){
		loc = boop;
		count = num;
	}
	
	public MapLocation getLoc(){
		return loc;
	}
	
	public long getCount(){
		return count;
	}
	
	public void changeCount(long c){
		count = c;
	}
	
	public long workerCollect(){
		if(count > 2){
			count -= 3;
		}
		else{
			count = 0;
		}
		
		return getCount();
	}

	public long getValue(MapLocation playerLocation){
		long val = playerLocation.distanceSquaredTo(loc);
		val -= count/2;
		return val;
	}
	
	
	
	/*
	 * getLoc
	 * getCount
	 * changeCount
	 * gardenerGet
	 * changeOwnership
	 */
}
