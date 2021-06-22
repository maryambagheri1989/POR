
package ptolemy.domains.atc.kernel.policy1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import ptolemy.actor.Actor;
import ptolemy.actor.FiringEvent;
import ptolemy.actor.IOPort;
import ptolemy.actor.Receiver;
import ptolemy.actor.util.Time;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.domains.atc.kernel.ATCReceiver_R;
import ptolemy.domains.atc.kernel.AirportFeilds;
import ptolemy.domains.atc.kernel.AreaSnapshot;
import ptolemy.domains.atc.kernel.DestinationAirportFields;
import ptolemy.domains.atc.kernel.GlobalSnapshot;
import ptolemy.domains.atc.kernel.TrackFields;
import ptolemy.domains.atc.lib.Airport_R;
import ptolemy.domains.atc.lib.ControlArea;
import ptolemy.domains.atc.lib.DestinationAirport_R;
import ptolemy.domains.atc.lib.Track_R;
import ptolemy.domains.de.kernel.DECQEventQueue;
import ptolemy.domains.de.kernel.DEEvent;
import ptolemy.domains.de.kernel.DEEventQueue;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Nameable;
import ptolemy.kernel.util.NamedObj;

public class InsideDirector extends ATCDirector_R{

    public InsideDirector(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public void initialize() throws IllegalActionException {
        // TODO Auto-generated method stub
        tracklists=new HashMap<>();
        sourceAirports=new HashMap<>();
        destinationAirports=new HashMap<>();
        
        removedEvents= new DECQEventQueue(
                ((IntToken) minBinCount.getToken()).intValue(),
                ((IntToken) binCountFactor.getToken()).intValue(),
                ((BooleanToken) isCQAdaptive.getToken())
                        .booleanValue());

        typeOfFiring=false;
        
        _director=(TopLevelDirector)((ControlArea)getContainer()).getExecutiveDirector();
        for(int i=0;i<_director.entities.size();i++)
            if(((IntToken)((ControlArea)_director.entities.get(i)).areaId.getToken()).intValue()==((IntToken)((ControlArea)getContainer()).areaId.getToken()).intValue())
                _areaSnapshotIndex=i;
        
        isItAcceptPhase=true;
        eventsOfPreboundaries=new ArrayList<>();
   
        super.initialize();
    }

    /**
     * Remove the given event from eventQueue
     * @param e
     * @throws IllegalActionException
     */
    public void removeEvent(DEEvent e) throws IllegalActionException {
        // TODO Auto-generated method stub
        _eventQueue.remove(e);
    }
    
    /**
     * add the given event to the removedEvents array
     * @param e
     * @throws IllegalActionException
     */
    public void saveEvent(DEEvent e) throws IllegalActionException {
        // TODO Auto-generated method stub
        removedEvents.put(e);
        
    }
    
    /**
     * If typeOfFiring is true we do not need to find the chains and removed events. 
     * This information should be stored in the current state. 
     * Also the timed states which are found are real timed states. 
     * Otherwise, they are internal timed states. 
     */
    @Override
    public void fire() throws IllegalActionException {
        // TODO Auto-generated method stub
      //fill removed events for the component if a parameter is false
        // and stores those information in the current state.
        //first clear remove events and next fill them
        // store internalTimedStates and timeReachableStates
        
        int areaId=((IntToken)((ControlArea)getContainer()).areaId.getToken()).intValue();
        eventsOfPreboundaries.clear();
        if(!typeOfFiring) {
            _findParallelEvents(areaId);
     
            if(!removedEvents.isEmpty())
            {
              //update events of the current state index
                Object[] eventArray=_eventQueue.toArray();
                _updateState();  
                _director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).eventQueue.clear();
                for(Object event:eventArray)
                    _director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).eventQueue.add((DEEvent)event);
                
                //update state of this area 
            }
            
            if(_eventQueue.isEmpty()) {
                //In this case we need to store the parallelEvents and also state of parallel actors.
               _restorEvents(_director.currentStateIndex);
               _storeActors(_director.currentStateIndex);
               _director.stateSpace.get(_director.currentStateIndex).currentCArea=((IntToken)((ControlArea)getContainer()).areaId.getToken()).intValue();
            }
            if(!_eventQueue.isEmpty() && !_checkForNextEvent() && !removedEvents.isEmpty()) {
                //In this case we need to store the parallelEvents and also state of parallel actors.
                
                //Set time of this model to the time of an event in removedEvents.
                setModelTime(removedEvents.get().timeStamp());
                
                _restorEvents(_director.currentStateIndex);
                _storeActors(_director.currentStateIndex);
                _director.stateSpace.get(_director.currentStateIndex).currentCArea=((IntToken)((ControlArea)getContainer()).areaId.getToken()).intValue();
            }
            if(!_director.order.contains(_areaSnapshotIndex))
                _director.order.add(_areaSnapshotIndex);
        }
        
        if(!_checkForNextEvent()) {
            if(!typeOfFiring) {
             // This shows that no new _internalTimedStates will be created in
             // this firing of the control area. But we have removed its state from _internalTimedStates,
                // while it should remain, because of firing of the next control areas.
                if(!_director._internalTimedStates.isEmpty()) {
                    GlobalSnapshot temp=new GlobalSnapshot(_director.stateSpace.get(_director.currentStateIndex).areas, _director.stateSpace.get(_director.currentStateIndex).index, _director.stateSpace.get(_director.currentStateIndex).upToThisTaken,_director.stateSpace.get(_director.currentStateIndex).name,
                            _director.stateSpace.get(_director.currentStateIndex).currentCArea, _director.stateSpace.get(_director.currentStateIndex).parent, _director.stateSpace.get(_director.currentStateIndex)._modelTime, _director.stateSpace.get(_director.currentStateIndex)._microstep, _director.stateSpace.get(_director.currentStateIndex).eventQueue, _director.stateSpace.get(_director.currentStateIndex).parallelEventQueue);
                    _director._internalTimedStates.add(temp);
                    _director.internalTimedStatesMap.put(temp, _director.currentStateIndex);
                }
            }
            return;
        }
            

        if (_debugging) {
            _debug("========= " + this.getName() + " director fires at "
                    + getModelTime() + "  with microstep as " + _microstep);
        }
        
        ArrayList<Integer[]> forkNodes=new ArrayList<Integer[]>(); 
        ArrayList<Integer> forkOfIntBoundaries=new ArrayList<Integer>();
        tempStates=new HashMap<>();
        //******************
        // NOTE: This fire method does not call super.fire()
        // because this method is very different from that of the super class.
        // A BIG while loop that handles all events with the same tag.
        while (true) {  
            int result = _fire();
            
           //MARYAM:
            if(_director.deadlockDetected==true)
                return;
           //
            
            assert result <= 1 && result >= -1;
            if (result == 1) {
                continue;
            } else if (result == -1) {
                _noActorToFire();
                return;
            } // else if 0, keep executing

            // after actor firing, the subclass may wish to perform some book keeping
            // procedures. However in this class the following method does nothing.
            _actorFired();
            //MARYAM:
            if (recentlyTimeStVisited!=-1 || !_checkForNextEvent()) {
                
                boolean flag=false;
                int parentIndex=-1;
                if(recentlyTimeStVisited==-1) {
                    // If this variable is not -1, then we are visiting a duplicative state
                    // in tempState and have to remove it from _stateSpace... Otherwise the state
                    // is a time state
                    if(!typeOfFiring) {
                        
                        //Store something about the final state when an internal boundary actor sends to the pre-boundary actor
//                        
                        if(_director.currentStateIndex==32)
                            _director.currentStateIndex=32;
                        _restorEvents(_director.currentStateIndex);
                        _storeActors(_director.currentStateIndex);
                        GlobalSnapshot temp=new GlobalSnapshot(_director.stateSpace.get(_director.currentStateIndex).areas, _director.stateSpace.get(_director.currentStateIndex).currentCArea, _director.stateSpace.get(_director.currentStateIndex).parent, _director.stateSpace.get(_director.currentStateIndex)._modelTime, _director.stateSpace.get(_director.currentStateIndex)._microstep, _director.stateSpace.get(_director.currentStateIndex).eventQueue, _director.stateSpace.get(_director.currentStateIndex).parallelEventQueue);
                        _director._internalTimedStates.add(temp);
                        _director.internalTimedStatesMap.put(temp, _director.currentStateIndex);
                    }
                    else {
                        int x=-1;
                        if(_director._timeReachableStatesMap.containsKey(_director.stateSpace.get(_director.currentStateIndex))) {
                            x=_director._timeReachableStatesMap.get(_director.stateSpace.get(_director.currentStateIndex)); //returns index in stateSpace array
                        }
//                        
                        if(x!=-1) { 
                            parentIndex=_director.stateSpace.get(_director.currentStateIndex).parent.get(0);
                            _cleanModel(_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).eventQueue);
                            _director.stateSpace.remove(_director.currentStateIndex);
                           _director.stateSpace.get(x).parent.add(parentIndex);
                        }
                        else {
                            _director._tempInternalTimedStates.add(_director.stateSpace.get(_director.currentStateIndex));
                            _director._tempInternalTimedStatesMap.put(_director.stateSpace.get(_director.currentStateIndex), _director.currentStateIndex);
                        }
                    }
                    
                }
                
                do{
                    if(parentIndex==-1) {
                        _cleanModel(_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).eventQueue);
                        _director.currentStateIndex=_director.stateSpace.get(_director.currentStateIndex).parent.get(0);
                    }
                    else {
                        _director.currentStateIndex=parentIndex;
                        parentIndex=-1;
                    }
                     
                    //If upToThisTaken of currentStateIndex refers to an internal boundary actor who want to send to a pre-boundary actor
                    int upToThis=_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).upToThisTaken;
                    if(!forkOfIntBoundaries.isEmpty() && forkOfIntBoundaries.get(forkOfIntBoundaries.size()-1)==_director.currentStateIndex)
                    {
                        forkOfIntBoundaries.remove(forkOfIntBoundaries.size()-1);
                    }
                    else {
                    if(upToThis!=-1)
                        if(_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).eventQueue.get(upToThis).ioPort()==null)
                            if(_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).eventQueue.get(upToThis).actor() instanceof Track_R) {
                                if(((StringToken)((Track_R)_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).eventQueue.get(upToThis).actor()).isBoundary.getToken())!=null &&
                                        ((StringToken)((Track_R)_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).eventQueue.get(upToThis).actor()).isBoundary.getToken()).stringValue().equals("inb")) {
                                    if(destPreBoundary(_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex),((Track_R)_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).eventQueue.get(upToThis).actor())))//if destination is pre-boundary
                                    {
                                        isItAcceptPhase=false;
                                        forkOfIntBoundaries.add(_director.currentStateIndex);
                                    }
                                    
                                }
                            }
                    }
                    
                    if(!forkNodes.isEmpty() && _director.currentStateIndex<forkNodes.get(forkNodes.size()-1)[0])
                    {
                        // We have passed the node in forkCounter and reached its parent
                        if(recentlyTimeStVisited!=-1) {
                            forkNodes.get(forkNodes.size()-1)[1]--;
                            int t=recentlyTimeStVisited;
                            while(forkNodes.get(forkNodes.size()-1)[0]!=t)
                            {
                                int parent=_director.stateSpace.get(t).parent.get(0);
                                _director.stateSpace.remove(t);
                                t=parent;
                                
                            }
                            recentlyTimeStVisited=-1;
                        }
                        if(forkNodes.get(forkNodes.size()-1)[1]==0)
                        {
                            //No child remains for this node and this node has to be removed.
                            recentlyTimeStVisited=forkNodes.get(forkNodes.size()-1)[0];
                            forkNodes.remove(forkNodes.size()-1);
//                            forkCounter--;
                        }
                    }
                    if(_director.stateSpace.get(_director.currentStateIndex).currentCArea!=areaId)
                    {
                        flag=true;
                        forkNodes.clear();
                        tempStates.clear();
                        break;
                    }
                } while(isItAcceptPhase==true && !_checkForNextEventInParent());
                 if(flag){
                    break;
                }
                
                if(!forkNodes.isEmpty() && forkNodes.get(forkNodes.size()-1)[0]==_director.currentStateIndex) //We've already added this fork  node
                {
                    if(recentlyTimeStVisited==-1){
                        Integer [] a={forkNodes.get(forkNodes.size()-1)[0],forkNodes.get(forkNodes.size()-1)[1]+1};
                        forkNodes.remove(forkNodes.size()-1);
                        forkNodes.add(a);
                    }
                    else{
                        int t=recentlyTimeStVisited;
                        while(_director.currentStateIndex!=t)
                        {
                            int parent=_director.stateSpace.get(t).parent.get(0);
                            _director.stateSpace.remove(t);
                            t=parent;
                            
                        }
                        recentlyTimeStVisited=-1;
                    }
                }
                else{
                    if(recentlyTimeStVisited==-1){
                        Integer [] a={_director.currentStateIndex,2};
                        forkNodes.add(a);
                    }
                    else{
                        Integer [] a={_director.currentStateIndex,1};
                        forkNodes.add(a);
                        int t=recentlyTimeStVisited;
                        while(_director.currentStateIndex!=t)
                        {
                            int parent=_director.stateSpace.get(t).parent.get(0);
                            _director.stateSpace.remove(t);
                            t=parent;
                            
                        }
                        recentlyTimeStVisited=-1;
                        
                    }
                }
                //Fill _eventQueue
                _eventQueue.clear();
                if(isItAcceptPhase==true) 
                    for(int i=_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).upToThisTaken+1;i<_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).eventQueue.size();i++)
                        _eventQueue.put(_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).eventQueue.get(i));
                else
                    for(int i=_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).upToThisTaken;i<_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).eventQueue.size();i++)
                        _eventQueue.put(_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).eventQueue.get(i));
                 
                // Fill MODEL
                try {
                    _fillModel(_director.stateSpace.get(_director.currentStateIndex));
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } // else keep executing in the current iteration
        } // Close the BIG while loop.
//        System.out.println("Total:" + counter);
        // Since we are now actually stopping the firing, we can set this false.
        _stopFireRequested = false;
        if (_debugging) {
            _debug("DE director fired!");
        }
    }
    
    /**
     * Since some events which should be taken in parallel are removed from event queue,
     * the state of the actor such as trackFields, airportFields,... should be updated. This function removes
     * the information of the actors referred to by the events in the removed events from those arrays, and also
     * update the index of the remaining actors in those arrays.
     */
    private void _updateState() {
        // TODO Auto-generated method stub
        Object[] eventQueue=_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).eventQueue.toArray();
        Object[] removed=removedEvents.toArray();
        int i=0;
        int k=0;
        int j=0;
        
        for(Object event:eventQueue) {
            DEEvent event2=(DEEvent) event;
            if(j<removed.length && event2.actor().getFullName().equals(((DEEvent)removed[j]).actor().getFullName())){
                j++;
                if(event2.actor() instanceof Track_R) {
                    _director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).trackActors.remove(i);
                } else if(event2.actor() instanceof Airport_R) {
                    _director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).airportActors.remove(i);
                }
                else if(event2.actor() instanceof DestinationAirport_R) {
                    _director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).destinationAirportActors.remove(i);
                }
            }
            else {
                if(event2.actor() instanceof Track_R) {
                    TrackFields temp=_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).trackActors.get(i);
                    _director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).trackActors.remove(i);
                    _director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).trackActors.put(k, new TrackFields(temp.called, temp.inTransit, temp.OutRoute, temp.transitExpires));
                } else if(event2.actor() instanceof Airport_R) {
                    AirportFeilds temp=_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).airportActors.get(i);
                    _director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).airportActors.remove(i);
                    _director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).airportActors.put(k, new AirportFeilds(temp._airplanes, temp._inTransit, temp._transitExpires));
                }
                else if(event2.actor() instanceof DestinationAirport_R) {
                    DestinationAirportFields temp=_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).destinationAirportActors.get(i);
                    _director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).destinationAirportActors.remove(i);
                    _director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).destinationAirportActors.put(k, new DestinationAirportFields(temp._inTransit, temp._transitExpires,temp._called));

                }
                k++;
            }
            i++;
        }
    }

    /**
     * This function finds the destination of a message sent from an internal boundary, and return true
     * if the destination is a pre-boundary actor
     * @param areaSnapshot 
     * @param track_R
     * @return
     */
    private boolean destPreBoundary(AreaSnapshot areaSnapshot, Track_R track) {
        // TODO Auto-generated method stub
        try {
            String map="";
            int id=((IntToken)track.trackId.getToken()).intValue();
            for(int i=0;i<areaSnapshot.eventQueue.size();i++) {
                if(areaSnapshot.eventQueue.get(i).actor() instanceof Track_R)
                if(((IntToken)((Track_R)areaSnapshot.eventQueue.get(i).actor()).trackId.getToken()).intValue()==id) {
                    map=((StringToken)((RecordToken)areaSnapshot.trackActors.get(i).inTransit).get("flightMap")).stringValue();
                    break;
                }
            }
             
            int dest=Integer.valueOf(map.split(";")[0].split(",")[0].split("/")[0].substring(1));
            if(destinationAirports.containsKey(dest))
            {
                return false;
            }
            if((StringToken)tracklists.get(dest).isBoundary.getToken()!=null && ((StringToken)tracklists.get(dest).isBoundary.getToken()).stringValue().equals("pb"))
            return true;
        } catch (IllegalActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    /**
     * This function store actors which should be run in parallel
     * @param stateIndex
     * @throws IllegalActionException
     */
    private void _storeActors(int stateIndex) throws IllegalActionException {
        // TODO Auto-generated method stub
      int counter=_eventQueue.size();
      for(int i=_director.stateSpace.get(stateIndex).index;i<_director.stateSpace.get(stateIndex).parallelEventQueue.size();i++) {
          
          NamedObj actor=(NamedObj) ((DEEvent)_director.stateSpace.get(stateIndex).parallelEventQueue.get(i)).actor();
          IOPort port=((DEEvent)_director.stateSpace.get(stateIndex).parallelEventQueue.get(i)).ioPort();
          if(port!=null){
              Receiver[][] receiver=port.getReceivers();
              Map<Integer, Token> temp=new TreeMap<Integer, Token>();
              //For each channel, check existence of the token in it. 
              for(int k=0;k<port.getWidth();k++){
                  if(port.hasNewToken(k)){
                      Token token=((ATCReceiver_R)receiver[k][0]).getToken();
                      temp.put(k,token);
                  }
              }
              // For one event, we check all channels of the port.
                  _director.stateSpace.get(stateIndex).areas.get(_areaSnapshotIndex).inputTokens.put(actor.getFullName()+port.getFullName(), temp);
          }
          
          if(actor instanceof  Track_R ){
              _director.stateSpace.get(stateIndex).areas.get(_areaSnapshotIndex).trackActors.put(counter, new TrackFields(
                      ((Track_R)actor)._called, ((Track_R)actor)._inTransit, ((Track_R)actor)._OutRoute, ((Track_R)actor)._transitExpires));
          }
          else if(actor instanceof Airport_R){
              _director.stateSpace.get(stateIndex).areas.get(_areaSnapshotIndex).airportActors.put(counter,new AirportFeilds(
                      ((Airport_R)actor)._airplanes,((Airport_R)actor)._inTransit, ((Airport_R)actor)._transitExpires));
          }
          else if(actor instanceof DestinationAirport_R){
              _director.stateSpace.get(stateIndex).areas.get(_areaSnapshotIndex).destinationAirportActors.put(counter,new DestinationAirportFields(((DestinationAirport_R)actor)._inTransit,
                      ((DestinationAirport_R)actor)._transitExpires, ((DestinationAirport_R)actor)._called));
          }
          counter++;
      }
        
    }

    /**
     * This function stores all events which should be processed in parallel in the parallelEventQueue.
     * It also change the type of a parallel event from pure to trigger, if a send from internal boundary to a 
     * pre-boundary is performed.
     * @param stateIndex
     */
    private void _restorEvents(int stateIndex) {
        // TODO Auto-generated method stub
        try {
            _director.stateSpace.get(stateIndex).index=_director.stateSpace.get(stateIndex).parallelEventQueue.size();
            HashMap<Integer, DEEvent> temp=new HashMap<>();
            Object[] eventArray=removedEvents.toArray();
            for(Object event: eventArray) { 
              
                if(((((DEEvent)event).actor()) instanceof Track_R) && ((StringToken)((Track_R)((DEEvent)event).actor()).isBoundary.getToken()).stringValue().equals("pb")) {
                    temp.put(((IntToken)((Track_R)((DEEvent)event).actor()).trackId.getToken()).intValue(), (DEEvent)event);
                }
                else
                    _director.stateSpace.get(stateIndex).parallelEventQueue.add((DEEvent)event);
            }
            
            for(DEEvent event:eventsOfPreboundaries)
            {
                _director.stateSpace.get(stateIndex).parallelEventQueue.add(event);
                
                //store token
                
                
                    if(temp.containsKey(((IntToken)((Track_R)event.actor()).trackId.getToken()).intValue()))
                        temp.remove(((IntToken)((Track_R)event.actor()).trackId.getToken()).intValue());
            }
            
            for(Entry<Integer,DEEvent> entry:temp.entrySet())
                _director.stateSpace.get(stateIndex).parallelEventQueue.add(entry.getValue());
        } catch (IllegalActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    private void _fillModel(GlobalSnapshot globalSnapshot) throws IllegalActionException, IllegalAccessException {
        // TODO Auto-generated method stub
        setModelTime(globalSnapshot._modelTime);
        _microstep=globalSnapshot._microstep;
        int i=0;
        for(DEEvent object: globalSnapshot.areas.get(_areaSnapshotIndex).eventQueue){
            NamedObj actor=(NamedObj)(object.actor());
            if(actor instanceof Track_R){
                _fillTrack(actor,globalSnapshot.areas.get(_areaSnapshotIndex).trackActors.get(i));
            }
            if(actor instanceof Airport_R){
                _fillAirport(actor, globalSnapshot.areas.get(_areaSnapshotIndex).airportActors.get(i));
            }
            if(actor instanceof DestinationAirport_R) {

                _fillDestinationAirport(actor, globalSnapshot.areas.get(_areaSnapshotIndex).destinationAirportActors.get(i));
            }
            
            IOPort port=object.ioPort();
            if(port!=null){
                String name=object.actor().getFullName()+port.getFullName();
                _fillInputPort(object,globalSnapshot.areas.get(_areaSnapshotIndex).inputTokens.get(name));
            }  
            i++;
        }
    }

    private void _cleanModel(ArrayList<DEEvent> eventQueue) throws IllegalActionException {
        // TODO Auto-generated method stub
        for(DEEvent object: eventQueue){
            NamedObj actor=(NamedObj) object.actor();
            
            IOPort port= object.ioPort();
            if(port!=null){
                Receiver[][] receiver=port.getReceivers();
                for(int i=0;i<receiver.length;i++){
                    for(int j=0;j<receiver[i].length;j++)
                        receiver[i][j].clear();
                } 
            }
            
            if(actor instanceof Track_R){
                ((Track_R)actor).cleanTrack();
                _inTransit.put(((IntToken)(((Track_R)actor).trackId.getToken())).intValue(), false);
            }
            
            if(actor instanceof Airport_R){
                ((Airport_R)actor)._airplanes.clear();
                ((Airport_R)actor)._inTransit=null;
            }
            
            if(actor instanceof DestinationAirport_R){
                ((DestinationAirport_R)actor).cleanDestinationAirport();
            }
        }
    }

    @Override
    protected int _fire() throws IllegalActionException {
        // Find the next actor to be fired.
        
        // If we have a timed transition, this state will be the target state, otherwise,
        // this will be the state which is obtained by firing an actor.
        // The information of this state will be updated. 
        _director.stateSpace.add(new GlobalSnapshot(_director.stateSpace.get(_director.currentStateIndex).currentCArea, _director.currentStateIndex, getModelTime(), _microstep,_director.stateSpace.get(_director.currentStateIndex).eventQueue, null));
        _nextStateIndex=_director.stateSpace.size()-1; // For time _nextStateIndex for immediate, current
        
        //***********************
        
        Actor actorToFire = _getNextActorToFire();
        
        
        
        //MARYAM
        if(actorToFire==null){
            _director.stateSpace.remove(_nextStateIndex);
        }
        
        _director._numberOfAllStates++;
        
        // Check whether the actor to be fired is null.
        // -- If the actor to be fired is null,
        // There are two conditions that the actor to be fired
        // can be null.
        if (actorToFire == null) {
            if (_isTopLevel()) {
                // Case 1:
                // If this director is an executive director at
                // the top level, a null actor means that there are
                // no events in the event queue.
                if (_debugging) {
                    _debug("No more events in the event queue.");
                }

                // Setting the following variable to true makes the
                // postfire method return false.
                // Do not do this if _stopFireRequested is true,
                // since there may in fact be actors to fire, but
                // their firing has been deferred.
                if (!_stopFireRequested) {
                    _noMoreActorsToFire = true;
                }
            } else {
                // Case 2:
                // If this director belongs to an opaque composite model,
                // which is not at the top level, the director may be
                // invoked by an update of an external parameter port.
                // Therefore, no actors contained by the composite model
                // need to be fired.
                // NOTE: There may still be events in the event queue
                // of this director that are scheduled for future firings.
                if (_debugging) {
                    _debug("No actor requests to be fired "
                            + "at the current tag.");
                }
            }
            // Nothing more needs to be done in the current iteration.
            // Simply return.
            // Since we are now actually stopping the firing, we can set this false.
            _stopFireRequested = false;
            return -1;
        }

        // NOTE: Here we used to check to see whether
        // the actor to be fired is the container of this director,
        // and if so, return to give the outside domain a chance to react
        // to that event. This strategy assumed that the
        // topological sort would always assign the composite actor the
        // lowest priority, which would guarantee that all the inside actors
        // have fired (reacted to their triggers) before the composite
        // actor is what is returned. However, the priority no longer
        // seems to always be lower. A better strategy is to continue
        // firing until we have exhausted all events with the current
        // tag and microstep.
        if (actorToFire == getContainer()) {
            /* What we used to do (before 5/17/09):
            // Since we are now actually stopping the firing, we can set this false.
            _stopFireRequested = false;
            return;
             */
            return 1;
        }

        if (_debugging) {
            _debug("****** Actor to fire: " + actorToFire.getFullName());
        }
        
        //MARYAM:
        for(int i=0;i<_director.entities.size();i++)
            _director.stateSpace.get(_nextStateIndex).areas.add(new AreaSnapshot());
        _director.stateSpace.get(_nextStateIndex).areas.get(_areaSnapshotIndex).name=actorToFire.getFullName();
        _director.stateSpace.get(_nextStateIndex).areas.get(_areaSnapshotIndex)._microstep=_microstep;
        _director.stateSpace.get(_nextStateIndex).areas.get(_areaSnapshotIndex)._modelTime=getModelTime();
        
        // Keep firing the actor to be fired until there are no more input
        // tokens available in any of its input ports with the same tag, or its prefire()
        // method returns false.
        boolean refire;

        do {
            refire = false;

            // NOTE: There are enough tests here against the
            // _debugging variable that it makes sense to split
            // into two duplicate versions.
            if (_debugging) {
                // Debugging. Report everything.
                // If the actor to be fired is not contained by the container,
                // it may just be deleted. Put this actor to the
                // list of disabled actors.
                if (!((CompositeEntity) getContainer())
                        .deepContains((NamedObj) actorToFire)) {
                    _debug("Actor no longer under the control of this director. Disabling actor.");
                    _disableActor(actorToFire);
                    break;
                }

                _debug(new FiringEvent(this, actorToFire,
                        FiringEvent.BEFORE_PREFIRE));

                if (!actorToFire.prefire()) {
                    _debug("*** Prefire returned false.");
                    break;
                }

                _debug(new FiringEvent(this, actorToFire,
                        FiringEvent.AFTER_PREFIRE));

                _debug(new FiringEvent(this, actorToFire,
                        FiringEvent.BEFORE_FIRE));

                actorToFire.fire();
                _debug(new FiringEvent(this, actorToFire,
                        FiringEvent.AFTER_FIRE));

                _debug(new FiringEvent(this, actorToFire,
                        FiringEvent.BEFORE_POSTFIRE));

                if (!actorToFire.postfire()) {
                    _debug("*** Postfire returned false:",
                            ((Nameable) actorToFire).getName());

                    // This actor requests not to be fired again.
                    _disableActor(actorToFire);
                    break;
                }

                _debug(new FiringEvent(this, actorToFire,
                        FiringEvent.AFTER_POSTFIRE));
            } else {
                // No debugging.
                // If the actor to be fired is not contained by the container,
                // it may just be deleted. Put this actor to the
                // list of disabled actors.
                if (!((CompositeEntity) getContainer())
                        .deepContains((NamedObj) actorToFire)) {
                    _disableActor(actorToFire);
                    break;
                }
                if (!actorToFire.prefire()) {
                    break;
                }

                actorToFire.fire();
                isItAcceptPhase=true;
                
                // NOTE: It is the fact that we postfire actors now that makes
                // this director not comply with the actor abstract semantics.
                // However, it's quite a redesign to make it comply, and the
                // semantics would not be backward compatible. It really needs
                // to be a new director to comply.
                if (!actorToFire.postfire()) {
                    // This actor requests not to be fired again.
                    _disableActor(actorToFire);
                    break;
                }

            }

            // Check all the input ports of the actor to see whether there
            // are more input tokens to be processed.
            // FIXME: This particular situation can only occur if either the
            // actor failed to consume a token, or multiple
            // events with the same destination were queued with the same tag.
            // In theory, both are errors. One possible fix for the latter
            // case would be to requeue the token with a larger microstep.
            // A possible fix for the former (if we can detect it) would
            // be to throw an exception. This would be far better than
            // going into an infinite loop.
            Iterator<?> inputPorts = actorToFire.inputPortList().iterator();

            while (inputPorts.hasNext() && !refire) {
                IOPort port = (IOPort) inputPorts.next();

                // iterate all the channels of the current input port.
                for (int i = 0; i < port.getWidth(); i++) {
                    if (port.hasNewToken(i)) {
                        if (_debugging) {
                            _debug("Port named " + port.getName()
                                    + " still has input on channel " + i
                                    + ". Refire the actor.");
                        }
                        // refire only if can be scheduled.
                        if (!_aspectsPresent || _schedule(
                                (NamedObj) actorToFire, getModelTime())) {
                            refire = true;

                            // Found a channel that has input data,
                            // jump out of the for loop.
                            break;
                        } else if (_aspectsPresent) {
                            if (_actorsInExecution == null) {
                                _actorsInExecution = new HashMap();
                            }
                            List<DEEvent> events = _actorsInExecution
                                    .get(actorToFire);
                            if (events == null) {
                                events = new ArrayList<DEEvent>();
                            }

                            events.add(new DEEvent(port, getModelTime(), 1,
                                    _getDepthOfActor(actorToFire)));
                            _actorsInExecution.put(actorToFire, events);
                        }
                    }
                }
            }
        } while (refire); // close the do {...} while () loop
        // NOTE: On the above, it would be nice to be able to
        // check _stopFireRequested, but this doesn't actually work.
        // In particular, firing an actor may trigger a call to stopFire(),
        // for example if the actor makes a change request, as for example
        // an FSM actor will do.  This will prevent subsequent firings,
        // incorrectly.
           

        int upToThis=_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).upToThisTaken;
        
        for(int i=0;i<upToThis;i++)
            _eventQueue.put(_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).eventQueue.get(i));
        
        Object[] objectArray=_eventQueue.toArray();
        for(Object object: objectArray)
            _director.stateSpace.get(_nextStateIndex).areas.get(_areaSnapshotIndex).eventQueue.add((DEEvent) object);
        
        _storeState(_director.currentStateIndex,_nextStateIndex);
        _director.currentStateIndex=_nextStateIndex;
        
        if(_director.currentStateIndex==48)
            _director.currentStateIndex=48;
      
        
        if(tempStates.containsKey(_director.stateSpace.get(_director.currentStateIndex)))
            recentlyTimeStVisited=_director.currentStateIndex;
        else
        {
            GlobalSnapshot temp= new GlobalSnapshot(_director.stateSpace.get(_director.currentStateIndex).areas, _director.stateSpace.get(_director.currentStateIndex).currentCArea, _director.stateSpace.get(_director.currentStateIndex).parent, _director.stateSpace.get(_director.currentStateIndex)._modelTime, _director.stateSpace.get(_director.currentStateIndex)._microstep, _director.stateSpace.get(_director.currentStateIndex).eventQueue, _director.stateSpace.get(_director.currentStateIndex).parallelEventQueue);
            tempStates.put(temp, _director.currentStateIndex);
        }
         return 0;
    }
    /**
     * This function copies the top-level eventQueue, and the other areas except for the current one.
     * Also it creates a new area for this control area, and stores all of its chains. It stores parallel event queues
     * but does not store the actors referred to by the parallel events.
     * @param currentStateIndex
     * @param _nextStateIndex2
     * @throws IllegalActionException 
     */
    private void _storeState(int currentStateIndex, int nextStateIndex) throws IllegalActionException {
        // TODO Auto-generated method stub
        _director.stateSpace.get(nextStateIndex).eventQueue=_director.stateSpace.get(currentStateIndex).eventQueue;
        _director.stateSpace.get(nextStateIndex).index=_director.stateSpace.get(currentStateIndex).index;
        
        for(int i=0;i<_director.stateSpace.get(currentStateIndex).parallelEventQueue.size();i++)
            _director.stateSpace.get(nextStateIndex).parallelEventQueue.add(_director.stateSpace.get(currentStateIndex).parallelEventQueue.get(i));
        
        int i=0;
        for(AreaSnapshot area:_director.stateSpace.get(currentStateIndex).areas) {
            if(i==_areaSnapshotIndex) {
 
                //Store actors
                int eventCounter=0;
                for(Object e: _eventQueue.toArray()) {
                    NamedObj actor=(NamedObj) ((DEEvent)e).actor();
                    IOPort port=((DEEvent)e).ioPort();
                    if(port!=null){
                        Receiver[][] receiver=port.getReceivers();
                        Map<Integer, Token> temp=new TreeMap<Integer, Token>();
                        //For each channel, check existence of the token in it. 
                        for(int k=0;k<port.getWidth();k++){
                            if(port.hasNewToken(k)){
                                Token token=((ATCReceiver_R)receiver[k][0]).getToken();
                                temp.put(k,token);
                            }
                        }
                        // For one event, we check all channels of the port.
                            _director.stateSpace.get(nextStateIndex).areas.get(i).inputTokens.put(actor.getFullName()+port.getFullName(), temp);
                    }
                    // End of storing tokens on the ports
                    if(actor instanceof Airport_R){
                        _director.stateSpace.get(nextStateIndex).areas.get(i).airportActors.put(eventCounter,new AirportFeilds(
                                ((Airport_R)actor)._airplanes,((Airport_R)actor)._inTransit, ((Airport_R)actor)._transitExpires));
                    }
                    else if(actor instanceof DestinationAirport_R) {
                        _director.stateSpace.get(nextStateIndex).areas.get(i).destinationAirportActors.put(eventCounter, new DestinationAirportFields(((DestinationAirport_R)actor)._inTransit, ((DestinationAirport_R)actor)._transitExpires, ((DestinationAirport_R)actor)._called));
                    }
                    else if(actor instanceof  Track_R ){
                        _director.stateSpace.get(nextStateIndex).areas.get(i).trackActors.put(eventCounter, new TrackFields(
                                ((Track_R)actor)._called, ((Track_R)actor)._inTransit, ((Track_R)actor)._OutRoute, ((Track_R)actor)._transitExpires));
                    }
                 eventCounter++;
                }
            }
            else {
                _director.stateSpace.get(nextStateIndex).areas.get(i).eventQueue=_director.stateSpace.get(currentStateIndex).areas.get(i).eventQueue;
                _director.stateSpace.get(nextStateIndex).areas.get(i)._modelTime=_director.stateSpace.get(currentStateIndex).areas.get(i)._modelTime;
                _director.stateSpace.get(nextStateIndex).areas.get(i)._microstep=_director.stateSpace.get(currentStateIndex).areas.get(i)._microstep;
                _director.stateSpace.get(nextStateIndex).areas.get(i).airportActors=_director.stateSpace.get(currentStateIndex).areas.get(i).airportActors;
                _director.stateSpace.get(nextStateIndex).areas.get(i).destinationAirportActors=_director.stateSpace.get(currentStateIndex).areas.get(i).destinationAirportActors;
                _director.stateSpace.get(nextStateIndex).areas.get(i).trackActors=_director.stateSpace.get(currentStateIndex).areas.get(i).trackActors;
                _director.stateSpace.get(nextStateIndex).areas.get(i).inputTokens=_director.stateSpace.get(currentStateIndex).areas.get(i).inputTokens;
                _director.stateSpace.get(nextStateIndex).areas.get(i).upToThisTaken=_director.stateSpace.get(currentStateIndex).areas.get(i).upToThisTaken;
                _director.stateSpace.get(nextStateIndex).areas.get(i).name=_director.stateSpace.get(currentStateIndex).areas.get(i).name;
                
            }
            i++;
            
        }
    }

    
    @Override
    protected Actor _getNextActorToFire() throws IllegalActionException {
        if (_eventQueue == null) {
            throw new IllegalActionException(
                    "Fire method called before the preinitialize method.");
        }
        Actor actorToFire = null;
        DEEvent lastFoundEvent = null;
        DEEvent nextEvent = null;
        // Keep taking events out until there are no more events that have the
        // same tag and go to the same destination actor, or until the queue is
        // empty, or until a stop is requested.
        // LOOPLABEL::GetNextEvent
        while (!_stopRequested) {
            // Get the next event from the event queue.
                if (_eventQueue.isEmpty()) {
                    // If the event queue is empty,
                    // jump out of the loop: LOOPLABEL::GetNextEvent
                    break;
                }
                nextEvent = _eventQueue.get();


            // This is the end of the different behaviors of embedded and
            // top-level directors on getting the next event.
            // When this point is reached, the nextEvent can not be null.
            // In the rest of this method, this is not checked any more.

            // If the actorToFire is null, find the destination actor associated
            // with the event just found. Store this event as lastFoundEvent and
            // go back to continue the GetNextEvent loop.
            // Otherwise, check whether the event just found goes to the
            // same actor to be fired. If so, dequeue that event and continue
            // the GetNextEvent loop. Otherwise, jump out of the GetNextEvent
            // loop.
            // TESTIT
            if (actorToFire == null) {
                // If the actorToFire is not set yet,
                // find the actor associated with the event just found,
                // and update the current tag with the event tag.
                Time currentTime;

                // Consume the earliest event from the queue. The event must be
                // obtained here, since a new event could have been enqueued
                // into the queue while the queue was waiting. Note however
                // that this would usually be an error. Any other thread that
                // posts events in the event queue should do so in a change request,
                // which will not be executed during the above wait.
                // Nonetheless, we are conservative here, and take the earliest
                // event in the event queue.
                synchronized (_eventQueueLock) {
                    lastFoundEvent = _eventQueue.take();
                    currentTime = lastFoundEvent.timeStamp();
                    actorToFire = lastFoundEvent.actor();

                    // Advance the current time to the event time.
                    // NOTE: This is the only place that the model time changes.
                    if(isItAcceptPhase==true) {
                        // only increase uptothistaken of the parent, if we are in the accept phase.
                    _director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex).upToThisTaken++;
                    }
                    
                    setModelTime(currentTime);

                    // Advance the current microstep to the event microstep.
                    _microstep = lastFoundEvent.microstep();
                    if (_debugging) {
                        _debug("Current time is: (" + currentTime + ", "
                                + _microstep + ")");
                    }
                    // Exceeding stop time means the current time is strictly
                    // bigger than the model stop time.
                    if (currentTime.compareTo(getModelStopTime()) > 0) {
                        if (_debugging) {
                            _debug("Current time has passed the stop time.");
                        }

                        _exceedStopTime = true;
                        return null;
                    }
                }
            } else { // i.e., actorToFire != null
                // In a previous iteration of this while loop,
                // we have already found an event and the actor to react to it.
                // Check whether the newly found event has the same tag
                // and destination actor. If so, they are
                // handled at the same time. For example, a pure
                // event and a trigger event that go to the same actor.
                if (nextEvent.hasTheSameTagAs(lastFoundEvent)
                        && nextEvent.actor() == actorToFire) {
                    // Consume the event from the queue and discard it.
                    // In theory, there should be no event with the same depth
                    // as well as tag because
                    // the DEEvent class equals() method returns true in this
                    // case, and the CalendarQueue class does not enqueue an
                    // event that is equal to one already on the queue.
                    // Note that the Repeat actor, for one, produces a sequence
                    // of outputs, each of which will have the same microstep.
                    // These reduce to a single event in the event queue.
                    // The DEReceiver in the downstream port, however,
                    // contains multiple tokens. When the one event on
                    // event queue is encountered, then the actor will
                    // be repeatedly fired until it has no more input tokens.
                    // However, there could be events with the same tag
                    // and different depths, e.g. a trigger event and a pure
                    // event going to the same actor.
                    synchronized (_eventQueueLock) {
                        DEEvent temp=_eventQueue.take();
                      //MARYAM
                        // Although this would not happen for us, because in our model
                        // every track actor is fired through only one event

                        _director.stateSpace.get(_director.currentStateIndex).upToThisTaken++;
                    }
                } else {
                    // Next event has a future tag or a different destination.
                    break;
                }
            }
        } // close the loop: LOOPLABEL::GetNextEvent
        // Note that the actor to be fired can be null.
        return actorToFire;
    }
    
    
    
    @Override
    protected boolean _checkForNextEvent() throws IllegalActionException {
        // The following code enforces that a firing of a
        // DE director only handles events with the same tag.
        // If the earliest event in the event queue is in the future,
        // this code terminates the current iteration.
        // This code is applied on both embedded and top-level directors.
        synchronized (_eventQueueLock) {
            if (!_eventQueue.isEmpty()) {
                DEEvent next = _eventQueue.get();

                if (next.timeStamp().compareTo(getModelTime()) > 0) {
                    // If the next event is in the future time,
                    // jump out of the big while loop in fire() and
                    // proceed to postfire().
                    return false;
                }  //*************This else if is removed by Maryam, because we do not need to compare microstep
                 else if (next.timeStamp().compareTo(getModelTime()) < 0) {
                    throw new IllegalActionException(
                            "The tag of the next event (" + next.timeStamp()
                                    + "." + next.microstep()
                                    + ") can not be less than"
                                    + " the current tag (" + getModelTime()
                                    + "." + _microstep + ") !");
                } else {
                    // The next event has the same tag as the current tag,
                    // indicating that at least one actor is going to be
                    // fired at the current iteration.
                    // Continue the current iteration.
                }
            } //************** Added by Maryam
            else{
                return false;
            }
            //**************
        }
        return true;
    }
    
    protected boolean _checkForNextEventInParent() throws IllegalActionException {
        // The following code enforces that a firing of a
        // DE director only handles events with the same tag.
        // If the earliest event in the event queue is in the future,
        // this code terminates the current iteration.
        // This code is applied on both embedded and top-level directors.
        
        AreaSnapshot area=_director.stateSpace.get(_director.currentStateIndex).areas.get(_areaSnapshotIndex);
        if(area.eventQueue.size()!=0 && area.upToThisTaken+1 < area.eventQueue.size()) {
            DEEvent next=area.eventQueue.get(area.upToThisTaken+1);
            if (next.timeStamp().compareTo(getModelTime()) > 0) {
                // If the next event is in the future time,
                // jump out of the big while loop in fire() and
                // proceed to postfire().
                return false;
            }  
            else if (next.timeStamp().compareTo(getModelTime()) < 0) {
                throw new IllegalActionException(
                        "The tag of the next event (" + next.timeStamp()
                                + "." + next.microstep()
                                + ") can not be less than"
                                + " the current tag (" + getModelTime()
                                + "." + _microstep + ") !");
            } else {
                // The next event has the same tag as the current tag,
                // indicating that at least one actor is going to be
                // fired at the current iteration.
                // Continue the current iteration.
            }
        }
        else {
            return false;
        }
        return true;
    }
    
    @Override
    public boolean postfire() throws IllegalActionException {
        // TODO Auto-generated method stub
        //Do nothing in this method .
        return true;
    }
    
    public void setEventQueue(ArrayList<DEEvent> eventQueue) throws IllegalActionException {
        // TODO Auto-generated method stub
        _eventQueue.clear();
        for(int i=0;i<eventQueue.size();i++)
            _eventQueue.put(eventQueue.get(i));
    }
    
    public Token findNeighbors(int trackNum) {
        // TODO Auto-generated method stub

        int row=-1;
        int column=-1;
        try {
            row = (((IntToken)((ControlArea)getContainer()).areaId.getToken()).intValue()-1)/(_director.dimension/_director.regionDimension);
            column=(((IntToken)((ControlArea)getContainer()).areaId.getToken()).intValue())%(_director.dimension/_director.regionDimension);
        } catch (IllegalActionException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        int northNeighbor=-1;
        int southNighbor=-1;
        int eastNeighbor=-1;
        try {
        String trackId=String.valueOf(trackNum);
        int regionSize=0;
        
        regionSize=String.valueOf(((IntToken)((ControlArea)getContainer()).areaId.getToken()).intValue()).length();
        
        trackNum=Integer.valueOf(trackId.substring(regionSize));
        
        
        if(trackNum-_director.regionDimension>0) {
            
          String x=String.valueOf(((IntToken)((ControlArea)getContainer()).areaId.getToken()).intValue())+(trackNum-_director.regionDimension);
          northNeighbor=Integer.valueOf(x);
        }
        else if(row==0)
            northNeighbor=-1;
        else {
            int desReg=calculateRegionNumber(row-1, column, _director.dimension, _director.regionDimension);
            String x= String.valueOf(desReg)+((_director.regionDimension*_director.regionDimension)-_director.regionDimension+trackNum);
            northNeighbor=Integer.valueOf(x);
        }
        
        if(trackNum%_director.regionDimension!=0) {
            String x=String.valueOf(((IntToken)((ControlArea)getContainer()).areaId.getToken()).intValue())+(trackNum+1);
            eastNeighbor=Integer.valueOf(x);
        }
        else if(column==0) {
//            eastNeighbor=-1;
            //destination airport
            int rowIn=trackNum/((IntToken)_director.dimentionOfRegion.getToken()).intValue();
            int y=((IntToken)_director.dimentionOfRegion.getToken()).intValue()*((IntToken)_director.dimentionOfRegion.getToken()).intValue()+
                    2*((IntToken)_director.dimentionOfRegion.getToken()).intValue()+(rowIn-1);
            String x=String.valueOf(((IntToken)((ControlArea)getContainer()).areaId.getToken()).intValue())+y;
            eastNeighbor=Integer.valueOf(x);
        }
        else {
            int desReg=calculateRegionNumber(row, column+1, _director.dimension, _director.regionDimension);
            String x= String.valueOf(desReg)+(trackNum-_director.regionDimension+1);
            eastNeighbor=Integer.valueOf(x);
        }
        
        if(trackNum+_director.regionDimension<=_director.regionDimension*_director.regionDimension) {
            String x=String.valueOf(((IntToken)((ControlArea)getContainer()).areaId.getToken()).intValue())+(trackNum+_director.regionDimension);
            southNighbor=Integer.valueOf(x);
        }
        else if(row==(_director.dimension/_director.regionDimension)-1) {
//            southNighbor=-1;
            int col=(trackNum-1)%_director.regionDimension;
            int y=((IntToken)_director.dimentionOfRegion.getToken()).intValue()*((IntToken)_director.dimentionOfRegion.getToken()).intValue()+
                    3*((IntToken)_director.dimentionOfRegion.getToken()).intValue()+col;
            String x=String.valueOf(((IntToken)((ControlArea)getContainer()).areaId.getToken()).intValue())+y;
            southNighbor=Integer.valueOf(x);
        }
        else {
            int desReg=calculateRegionNumber(row+1, column, _director.dimension, _director.regionDimension);
            String x= String.valueOf(desReg)+((trackNum+_director.regionDimension)%(_director.regionDimension*_director.regionDimension));
            southNighbor=Integer.valueOf(x);
        }
        } catch (IllegalActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        String x="{"+northNeighbor+","+eastNeighbor+","+southNighbor+"}";
        ArrayToken allNeighbors=null;
        try {
            allNeighbors = new ArrayToken(x);
        } catch (IllegalActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return (Token)allNeighbors;
    }
    
    public RecordToken createNewMObject(RecordToken aircraftWithInformation, double consumedFuel) throws IllegalActionException {
        Map<String, Token> newAircraft = new TreeMap<String, Token>();
        newAircraft.put("aircraftId",
                aircraftWithInformation.get("aircraftId"));
        newAircraft.put("aircraftSpeed",
                aircraftWithInformation.get("aircraftSpeed"));
        newAircraft.put("flightMap",
                aircraftWithInformation.get("flightMap"));
        newAircraft.put("priorTrack",
                aircraftWithInformation.get("priorTrack"));
       double fuel=((DoubleToken) aircraftWithInformation.get("fuel")).doubleValue()-consumedFuel;
       if(fuel<=0)
          ((TopLevelDirector)((ControlArea)getContainer()).getExecutiveDirector()).deadlockDetected=true;
//           throw new IllegalActionException("deadlock: the airplane "+aircraftWithInformation.get("aircraftId")+ " With route "+aircraftWithInformation.get("flightMap"));
       newAircraft.put("fuel", new DoubleToken(fuel));
        return new RecordToken(newAircraft);
    }
    
    public void setTrackInArray(Track_R track) throws IllegalActionException {
        // TODO Auto-generated method stub
        tracklists.put(((IntToken)track.trackId.getToken()).intValue(), track);
    }
    
    public void setAirportInArray(Airport_R airport) throws IllegalActionException {
        // TODO Auto-generated method stub
        sourceAirports.put(((IntToken)airport.airportId.getToken()).intValue(), airport);
    }
    
    public void setDestinationAirport(DestinationAirport_R destAirport) throws IllegalActionException {
        destinationAirports.put(((IntToken)destAirport.airportId.getToken()).intValue(), destAirport);
    }
    
    
    public int calculateRegionNumber(int row, int column, int nd, int rd) {
        if(column==0)
            return ((nd/rd)*row)+(nd/rd);
        else 
            return ((nd/rd)*row)+column;
    }
    
    public void addEvent(Object object) {
        // TODO Auto-generated method stub
        try {
            _eventQueue.put((DEEvent)object);
        } catch (IllegalActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    
    /**
     * When a component is going to be fired we first find the set of events which should be selected in parallel with other areas.
     * In each iteration of the topLevelDirector,
     * and per each component, this function is only called once. Furthermore, we remove the events 
     * from the event queue of the component.
     * @param controlArea
     * @param eventQueue
     * @throws IllegalActionException
     */
   void _findParallelEvents(int controlArea) throws IllegalActionException {
      
        Object[] objectArray=_eventQueue.toArray();
        
        int counter=0;
        for(Object object:objectArray) {
            DEEvent e=(DEEvent) object;
            if(e.timeStamp().equals(getModelTime()) && _director.inParallelEvent(e)) {
                NamedObj actor=(NamedObj) e.actor();
//                if(actor instanceof Track_R)
//                    parallelActors.put(counter, new TrackFields(
//                        ((Track_R)actor)._called, ((Track_R)actor)._inTransit, ((Track_R)actor)._OutRoute, ((Track_R)actor)._transitExpires));
//                else if(actor instanceof Airport_R)
//                    parallelAirports.put(counter, new AirportFeilds(((Airport_R)actor)._airplanes, ((Airport_R)actor)._inTransit, ((Airport_R)actor)._transitExpires));
                removeEvent(e);
                saveEvent(e);
               counter++;
            }
        }
        
    }
   
   public void clearEventQueue() {
       // TODO Auto-generated method stub
       _eventQueue.clear();
   }
   
    /**
     * List of tracks in this component
     */
    public HashMap<Integer, Track_R> tracklists;
    
    /**
     * List of source airports in this component
     */
    public HashMap<Integer, Airport_R> sourceAirports;
    
    /**
     * List of destination airports in this component
     */
    public HashMap<Integer, DestinationAirport_R> destinationAirports;
    
    /**
     * To store the sets of event which should be taken in parallel
     */
    DEEventQueue removedEvents;
    
//    /**
//     * We keep the set of internal and external chains per each region and between
//     * two time steps. During a time step, the sets of chains are changed. **Please be 
//     * noticed that chainsPerRegions array is cleared at the end of each time step.
//     */
//    ChainClass chainsPerRegions;
    
    /**
     *  To store states generated between two timed transitions
     */
    private HashMap<GlobalSnapshot, Integer> tempStates;
    
    private int recentlyTimeStVisited=-1;
    
    private int _nextStateIndex;
    
    /**
     * If typeOfFiring is true, means that we do not need to find the chains and removed events. 
     * Also the final states created in this component are real timed states and should be stored in 
     * timedReachableStates and timedReachableStatesMap. Otherwise, they are stored in internalTimedStates and internalTimedStatesMap.
     */
    public boolean typeOfFiring;
    
    public TopLevelDirector _director;
    
    /**
     * The areas are stored in a global state in order of their corresponding entities in entity list of the top-level director
     */
    public int _areaSnapshotIndex;
    
    
    /**
     * This variable is true when we fire a internal boundary actor for the first time (when we are 
     * generating the state space in the forward manner). When we backtrack, this variable should be set to false for a given actor.
     */
   
    public boolean isItAcceptPhase;

    /**
     * This eventQueue is used to store the events which are generated in the ATCReceiver_R
     * when an internal boundary is executed. When the sequential part of a component is executed
     * this array is filled and stored at parallelEventQueue of the final states which are generated at that phase.
     */
    public ArrayList<DEEvent> eventsOfPreboundaries;

  
    


    

}
