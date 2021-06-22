// Each ATC region
package ptolemy.domains.atc.kernel;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import ptolemy.domains.atc.lib.Track_NR;


public class NRRegion {

    
    public NRRegion(int id) {
        super();
        underAnalysis=false;
        this.id=id;
        tracks=new TreeMap<>();
        travelingAicrafts= new TreeMap<Integer, String>();
        entryTracks=new TreeMap<>();
        departureTracks=new TreeMap<>();
        northNeighbors=new TreeMap<>();
        westNeighbors=new TreeMap<>();
        eastNeighbors=new TreeMap<>();
        southNeighbors=new TreeMap<>();
        aircraftHasArrived= new ArrayList();
        neighbors=new TreeMap();
        numInputfrom=new TreeMap<>();
        arrivalTimeAtRegion=new TreeMap<>();
        departureTimeFromRegion=new TreeMap<>();

    }
    public NRRegion(int id, Map<Integer, Track_NR> tracks, Map<Integer, String> travelingAicrafts,
            Map<Integer, Integer> entryTracks, Map<Integer, Integer> departureTracks, NRRegion northRegion,
            NRRegion westRegion, NRRegion southRegion, NRRegion eastRegion) {
        super();
        underAnalysis=false;
        this.id=id;
        this.tracks = tracks;
        this.travelingAicrafts = travelingAicrafts;
        this.entryTracks = entryTracks;
        this.departureTracks = departureTracks;
    }
    public  boolean underAnalysis;
    public int id;
    
    public Map<Integer, Track_NR> tracks;
  //  public ArrayList<Track> tracks;
    // The schedule of the aircraft traveling through this region.
    // public Map<Integer,ArrayToken> travelingAicrafts;
    public Map<Integer,String> travelingAicrafts;
    
    // If the aircraft arrives at this region at its pre-determined time, we save
    // it. Only the aircraft which arrives at its time is stored.
    // otherwise, the aircraft has not arrived yet or has not arrived at its time.
    public ArrayList<Integer> aircraftHasArrived;
    
    // The aircraft reach to this region trough which track. If it does not have an entry track
    // it is not stored.
    public Map<Integer, Integer> entryTracks;
    // The aircraft arrives at which track after departing from this region
    public Map<Integer, Integer> departureTracks;
    // Tracks placed in the north of this region
    
    public Map<Integer, Double> arrivalTimeAtRegion;
    
    public Map<Integer, Double> departureTimeFromRegion;
    
    public Map<Integer, Track_NR> northNeighbors;
    public Map<Integer, Track_NR> westNeighbors;
    public Map<Integer, Track_NR> eastNeighbors;
    public Map<Integer, Track_NR> southNeighbors;
    
    public Map<Integer,NRRegion> neighbors; //0:North, 1: East, 2: South, 3: West
    
    /**The key is the id of the region which sends inputs to "this" region, and the
     * value is the number of aircraft which are send through that region to the current region.*/
    public Map<Integer, Integer> numInputfrom;
    
    public Track_NR _containsTrack(Map<Integer, Track_NR> array, int prevTrack) {
        // TODO Auto-generated method stub
        if(array.containsKey(prevTrack))
            return array.get(prevTrack);
        return null;
    }
    
    
}
