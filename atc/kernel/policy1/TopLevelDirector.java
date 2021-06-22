/**
 * Some parts of the code only works for this rejection algorithm.
 * For instance, _parallelExecutionOfDeepActors and removeStatesUptoRejection.
 * The reason for the first function is that, if the first state in the _internalTimedStates
 * does not have events to be executed in parallel, it does not mean that the other ones also do not have
 * Parallel events (because an internal boundary actor may be rejected and sends its token to a pre-boundary), 
 * or when the first one has, it does not mean that the other ones also have parallel events.
 * But because in this re-routing algorithm, the token holds its position, this problem does not happen. 
 */
package ptolemy.domains.atc.kernel.policy1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import ptolemy.actor.Actor;
import ptolemy.actor.CompositeActor;
import ptolemy.actor.Director;
import ptolemy.actor.FiringEvent;
import ptolemy.actor.IOPort;
import ptolemy.actor.Manager;
import ptolemy.actor.Receiver;
import ptolemy.actor.SuperdenseTimeDirector;
import ptolemy.actor.util.Time;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.domains.atc.kernel.ATCReceiver_R;
import ptolemy.domains.atc.kernel.AirportFeilds;
import ptolemy.domains.atc.kernel.AreaSnapshot;
import ptolemy.domains.atc.kernel.DestinationAirportFields;
import ptolemy.domains.atc.kernel.GlobalSnapshot;
import ptolemy.domains.atc.kernel.TrackFields;
import ptolemy.domains.atc.kernel.info;
import ptolemy.domains.atc.lib.Airport_R;
import ptolemy.domains.atc.lib.ControlArea;
import ptolemy.domains.atc.lib.DestinationAirport_R;
import ptolemy.domains.atc.lib.TimedStatesList;
import ptolemy.domains.atc.lib.Track_R;
import ptolemy.domains.de.kernel.DECQEventQueue;
import ptolemy.domains.de.kernel.DEDirector;
import ptolemy.domains.de.kernel.DEEvent;
import ptolemy.domains.de.kernel.DEEventQueue;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InvalidStateException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Nameable;
import ptolemy.kernel.util.NamedObj;

public class TopLevelDirector extends DEDirector {

    public TopLevelDirector(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        // TODO Auto-generated constructor stub
        
        networkDimension = new Parameter(this, "networkDimension");
        networkDimension.setExpression("3");
        networkDimension.setTypeEquals(BaseType.INT);
        
        dimentionOfRegion = new Parameter(this, "dimentionOfRegion");
        dimentionOfRegion.setExpression("3");
        dimentionOfRegion.setTypeEquals(BaseType.INT);
        
        indexOfFile = new Parameter(this, "indexOfFile");
        indexOfFile.setExpression("0");
        indexOfFile.setTypeEquals(BaseType.INT);
    }
    
    public Parameter networkDimension;
    public Parameter dimentionOfRegion;
    public Parameter indexOfFile;
    
    @Override
    public void initialize() throws IllegalActionException {
        // TODO Auto-generated method stub
        
        CompositeActor container = (CompositeActor) getContainer();
        entities=container.deepOpaqueEntityList();
        numberOfRegions= entities.size();
        currentUderAnalysisRegion=-1;
        _internalTimedStates=new ArrayList<>();
        internalTimedStatesMap=new HashMap<>();
        stateSpace=new ArrayList<>();
        _timeReachableStatesMap=new HashMap<>();
        _timeReachableStates=new TimedStatesList(null, null);
        deadlockDetected=false;
        timeOver=false;
        currentStateIndex=-1;
        _currentTimedState=null;
        dimension=((IntToken)networkDimension.getToken()).intValue();
        regionDimension=((IntToken)dimentionOfRegion.getToken()).intValue();
        fileRead=false;
        hasBeenAccepted=false;
        rejected=false;
        statesHasToRemoved=new HashMap<>();
        isRejectPhase=false;
        
        boundaryTrack=null;
        stateInAfterTime=0;
        preToInternalB=new HashMap<>();
        
        _internalEventQueue=new DECQEventQueue(
                ((IntToken) minBinCount.getToken()).intValue(),
                ((IntToken) binCountFactor.getToken()).intValue(),
                ((BooleanToken) isCQAdaptive.getToken())
                        .booleanValue());
        
        _recentlyStVisited=-1;
        _internalStopFireRequested=false;
        _nextStateIndex=-1;
        
        _numberOfAllStates=0;
        _numberOfTimedStates=0;
        
        for(int i=0;i<entities.size();i++)
           ((InsideDirector)((ControlArea)entities.get(i)).getDirector())._areaSnapshotIndex=i;
        
        order=new ArrayList<>();
        test=0;

        super.initialize();
    }
    
    @Override
    public void wrapup() throws IllegalActionException {
        // TODO Auto-generated method stub
        String outputFileStates="";
        String outputFileTime="";
        String outputFileAllStates="";
        String outputTimedStates="";
        
        if(((IntToken)indexOfFile.getToken()).intValue()==0) {
            outputFileStates="outputS.txt";
            outputFileTime="outputT.txt";
            outputFileAllStates="outputAS.txt";
            outputTimedStates="outputTimedStates.txt";
        }
        else {
            outputFileStates="outputS"+((IntToken)indexOfFile.getToken()).intValue()+".txt";
            outputFileTime="outputT"+((IntToken)indexOfFile.getToken()).intValue()+".txt";
            outputFileAllStates="outputAS"+((IntToken)indexOfFile.getToken()).intValue()+".txt";
            outputTimedStates="outputTimedStates"+((IntToken)indexOfFile.getToken()).intValue()+".txt";
        }
        


        super.wrapup();
        _disabledActors = null;
        synchronized (_eventQueueLock) {
            _eventQueue.clear();
        }
        _noMoreActorsToFire = false;
        _microstep = 0;
        try {

              File file=new File(outputFileStates);
              File fileTime=new File(outputFileTime);
              File fileAllStates=new File(outputFileAllStates);
              File fileTimedStates=new File(outputTimedStates);
            

            FileWriter writer = new FileWriter(file, true);
            PrintWriter _writer =new PrintWriter(writer);
            
            FileWriter writerTime = new FileWriter(fileTime, true);
            PrintWriter _writerTime =new PrintWriter(writerTime);
            
            FileWriter writerAllStates = new FileWriter(fileAllStates, true);
            PrintWriter _writerAllStates =new PrintWriter(writerAllStates);
            
            FileWriter writeTimedStates = new FileWriter(fileTimedStates, true);
            PrintWriter _writerTimedStates =new PrintWriter(writeTimedStates);

            if(deadlockDetected) {
                _writerTime.print("deadlock"+"\n");
                _writer.print("deadlock"+"\n");
            }
            else if(timeOver)
            {
                _writerTime.print("timeOver"+"\n");
                _writer.print("timeOver"+"\n");
            }
            else {
                if(startTime==0)
                    _writerTime.print(0+"\n");
                else {
                   _writerTime.print((System.currentTimeMillis()-startTime)+"\n");

                }
                int x=0;
                int y=0;
                if(!statesHasToRemoved.isEmpty()) {
                    x=statesHasToRemoved.size();
                }
                y=(stateSpace.size()-x);
                _writer.print(y+"\n");
                System.out.println("Number of states: "+y+"\n");
                y=(_numberOfAllStates-x);
                _writerAllStates.print(y+"\n");
                _writerTimedStates.print(_numberOfTimedStates+"\n");
                System.out.println("The number of all States: "+y+"\n");
                System.out.println("The number of timed States: "+_numberOfTimedStates+"\n");
             
            }
            _writerTime.close();
            

            _writer.close();
            _writerAllStates.close();
            _writerTimedStates.close();
            deadlockDetected=false;
            timeOver=false;
            stateSpace.clear();
            _timeReachableStates.clear();
            _timeReachableStatesMap.clear();
            _numberOfAllStates=0;
            _numberOfTimedStates=0;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    

    
    @Override
    public void fire() throws IllegalActionException {
        if(fileRead==false) {
            fileRead=true;
            try {
                _readFromFile();
                startTime=System.currentTimeMillis(); 
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        dependentActors=new HashMap<>();
        
        if (_debugging) {
            _debug("========= " + this.getName() + " director fires at "
                    + getModelTime() + "  with microstep as " + _microstep);
        }
        // NOTE: This fire method does not call super.fire()
        // because this method is very different from that of the super class.
        // A BIG while loop that handles all events with the same tag.
        while (true) { 
            int result = _fire();
            
            //MARYAM:
            if(deadlockDetected==true)
                return;
            GlobalSnapshot temp=null;
            int index=-1;
            //MARYAM: Traversing using BFS. 
            if(!_internalTimedStates.isEmpty())
            {
                _eventQueue.clear();
                temp=_internalTimedStates.get(0);
                index=currentStateIndex;
                currentStateIndex=internalTimedStatesMap.get(temp);
                for(int i=0;i<temp.eventQueue.size();i++)
                    _eventQueue.put(_internalTimedStates.get(0).eventQueue.get(i));
            }
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
            if (!_checkForNextEvent()) {
                if(index!=-1)
                    _clearModelOverITStates(index);
                break;
            } // else keep executing in the current iteration
            
            //MARYAM: The global timed state is not removed from the 
            // _internalTimedStates.
          //MARYAM: Traversing using BFS. 
            
            if(!_internalTimedStates.isEmpty())
            {
                _clearModelOverITStates();
                temp=_internalTimedStates.get(0);
                currentStateIndex=internalTimedStatesMap.get(temp);
                stateInAfterTime=currentStateIndex;
                try {
                    _fillModelWithITStates(temp);
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                _internalTimedStates.remove(0);
                internalTimedStatesMap.remove(temp);
            }
            //
        } // Close the BIG while loop.
        
        //MARYAM: When we reach this point, the model has been filled with the first timed 
        // state in the _internalTimedStates. Up to this place, the states which are
        //stored in the _internalTimedStates are different. Now we should fire the actors in parallel.
        
        if(_internalTimedStates.isEmpty() && currentStateIndex==stateInAfterTime) {
            //this shows that all events in the fired composite actors should be taken in parallel 
            // and no states are generated during their firing.
            stateSpace.get(stateSpace.get(currentStateIndex).parent.get(0)).currentCArea=-1;
        }
        else {
            //This is done to diagnose when we are passing from root of the parallel execution up.
            int tempIndex=internalTimedStatesMap.get(_internalTimedStates.get(0));
    
            stateSpace.get(_internalTimedStates.get(0).parent.get(0)).currentCArea=-1;
    
            currentStateIndex=tempIndex;
        }
        _changeState(currentStateIndex);
        _parallelExecutionOfDeepActors();
        
        // Since we are now actually stopping the firing, we can set this false.
        _stopFireRequested = false;

        if (_debugging) {
            _debug("DE director fired!");
        }
    }
    
    private void _clearModelOverITStates(int index) throws IllegalActionException {
        // TODO Auto-generated method stub
        int k=0;
        for(Object entity:entities) {
            ((InsideDirector)((ControlArea) entity).getDirector()).clearEventQueue();

                Object[] eventArray=stateSpace.get(index).areas.get(k).eventQueue.toArray();
                for(Object object: eventArray ){
                    NamedObj actor=(NamedObj) ((DEEvent)object).actor();
                    IOPort port= ((DEEvent)object).ioPort();
                    if(port!=null){
                        Receiver[][] receiver=port.getReceivers();
                        for(int i=0;i<receiver.length;i++){
                            for(int j=0;j<receiver[i].length;j++)
                                receiver[i][j].clear();
                        } 
                    }
                    if(actor instanceof Track_R){
                        ((Track_R)actor).cleanTrack();
                        ((InsideDirector)(((ControlArea)entity).getDirector()))._inTransit.put(((IntToken)(((Track_R)actor).trackId.getToken())).intValue(), false);
                    }
                    if(actor instanceof Airport_R){
                        ((Airport_R)actor)._airplanes.clear();
                        ((Airport_R)actor)._inTransit=null;
                    }
                    if(actor instanceof DestinationAirport_R){
                        ((DestinationAirport_R)actor).cleanDestinationAirport();
                    }
    
                }

          k++;  
         }
        
        
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
                /*
                else if (next.microstep() > _microstep) {
                    // If the next event has a bigger microstep,
                    // jump out of the big while loop in fire() and
                    // proceed to postfire().
                    return false;
                } */
                //********** The else if below is changed by Maryam
                //else if (next.timeStamp().compareTo(getModelTime()) < 0
                     //  || next.microstep() < _microstep) {
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
    
    /**
     * This function is used to clear the model before filling it with an internalTimedStates, while we want
     * to fire the next component.
     * @param globalSnapshot
     * @throws IllegalActionException 
     */
    private void _clearModelOverITStates() throws IllegalActionException {
        // TODO Auto-generated method stub
        _eventQueue.clear();
        int k=0;
        for(Object entity:entities) {
           //((InsideDirector)((ControlArea) entity).getDirector()).removedEvents.clear();
            ((InsideDirector)((ControlArea) entity).getDirector()).clearEventQueue();
            Object[] eventArray=stateSpace.get(currentStateIndex).areas.get(k).eventQueue.toArray();
           for(Object object: eventArray ){
               NamedObj actor=(NamedObj) ((DEEvent)object).actor();
               IOPort port= ((DEEvent)object).ioPort();
               if(port!=null){
                   Receiver[][] receiver=port.getReceivers();
                   for(int i=0;i<receiver.length;i++){
                       for(int j=0;j<receiver[i].length;j++)
                           receiver[i][j].clear();
                   } 
               }
               if(actor instanceof Track_R){
                   ((Track_R)actor).cleanTrack();
                   ((InsideDirector)((ControlArea) entity).getDirector())._inTransit.put(((IntToken)(((Track_R)actor).trackId.getToken())).intValue(), false);
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
        
    }

    /**
     * This function is used to fill model with timed internal states, while we want to fire next component.
     * @param temp
     * @throws IllegalActionException 
     * @throws IllegalAccessException 
     */
    private void _fillModelWithITStates(GlobalSnapshot temp) throws IllegalActionException, IllegalAccessException {
        // TODO Auto-generated method stub
        for(int i=0;i<temp.eventQueue.size();i++)
            _eventQueue.put(temp.eventQueue.get(i));
        
        int k=0;
        for(Object entity:entities) {  
            ((InsideDirector)((ControlArea)entity).getDirector()).setEventQueue(temp.areas.get(k).eventQueue);
            int j=0;
            for(DEEvent object: temp.areas.get(k).eventQueue){
                NamedObj actor=(NamedObj)(object.actor());
                if(actor instanceof Track_R){
                    ((InsideDirector)((ControlArea)entity).getDirector())._fillTrack(actor,temp.areas.get(k).trackActors.get(j));
                }
                if(actor instanceof Airport_R){
                    ((InsideDirector)((ControlArea)entity).getDirector())._fillAirport(actor, temp.areas.get(k).airportActors.get(j));
                }
                if(actor instanceof DestinationAirport_R) {
                    ((InsideDirector)((ControlArea)entity).getDirector())._fillDestinationAirport(actor, temp.areas.get(k).destinationAirportActors.get(j));
                }
                j++;
            }
            k++;
        }
        
    }
 
    private void _parallelExecutionOfDeepActors() throws IllegalActionException {
        // TODO Auto-generated method stub
        //for each boundary send, add an event to fire the receiver actor.
        //also transfer the input to the receiver
        // also the receiver of the top level director in some cases should call
        // the receiver of the internal actor
        // also if an event is added to the global director to fire a component,
        // we will clear the global queue and refill it at start of its iteration. // We do not allow 
        // that the event is added to the global queue.
        // if some actors in a queue can be executed sequentially execute them sequentially.
        // the final states are timed states which are stored in the timed states array
        // This director needs a new receiver, when a send occurs on boundary, the put function of this
        // Receiver is called and an event is generated for this coordinator. the event for the receiver actor
        // in the other component is generated when director.transfer input in the fire function of the component is called.
        
        
       
      
        try {
            _fillModelInternalFire(stateSpace.get(currentStateIndex));
        } catch (IllegalAccessException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
//        First the removed events of each region is restored.
        _storeEvents(currentStateIndex);
        
        if(!_internalEventQueue.isEmpty()) {

            inParallelExecution=true;
            
            boolean flag=true;
            while(flag) {
                internalFire();
                if(deadlockDetected)
                    return;
                flag=internalPostFire();   
                if(timeOver)
                    return;
            }
            inParallelExecution=false;
        }
        else { //Since there are not events in parallel event queue to be taken in parallel,
            // the internaltimedstates are real timed states.
            // model has been filled.
            // if the first state does not have parallel events, the first state is not a wrong state.
            _cleanModelInternalFire(currentStateIndex);
            for(int i=0;i<_internalTimedStates.size();i++)
            {
                GlobalSnapshot temp=_internalTimedStates.get(i);
                int index=internalTimedStatesMap.get(temp);
                internalTimedStatesMap.remove(temp);
                
              //Add first event of each internal queue to the top-level queue
                for(int j=0;j<_internalTimedStates.get(i).areas.size();j++) {
                    AreaSnapshot area=_internalTimedStates.get(i).areas.get(j);
                    if(!area.eventQueue.isEmpty()) {
                        DEEvent e=area.eventQueue.get(0);
                        Actor a=(Actor) ((ControlArea)e.actor().getContainer());
                        
                        Time result = e.timeStamp();
                        if (result.compareTo(getModelTime()) < 0) {
                            // NOTE: There is no assurance in a multithreaded situation
                            // that time will not advance prior to the posting of
                            // this returned time on the local event queue, so
                            // an exception can still occur reporting an attempt
                            // to post an event in the past.
                            result = getModelTime();
                        }
                        
                        DEEvent event=new DEEvent(a, result, 0, 0);
                        
                       _internalTimedStates.get(i).eventQueue.add(event); //we do not need to add this state to
                       //internalTimedStatesMap because the next phase is post fire in which the arrays are removed
                    }
                    
                }
              
                
                temp=_internalTimedStates.get(i); //We do not need to add 
                stateSpace.set(index, temp);
            
                
                _timeReachableStates.add(index, temp._modelTime);
                _timeReachableStatesMap.put(temp, index);
                
                _numberOfTimedStates++;
            }
        }
  
    }

    
    /**
     * Store events of the parallel eventqueue into the model
     * @param currentStateIndex2
     */
    private void _storeEvents(int stateIndex) {
        // TODO Auto-generated method stub
        for(DEEvent event: stateSpace.get(stateIndex).parallelEventQueue)
            try {
                _internalEventQueue.put(event);
            } catch (IllegalActionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }

    /**
     * In this method we only fill the actors. There is no input tokens. 
     * @param stateIndex
     */
    private void _fillActors(int stateIndex) {
        // TODO Auto-generated method stub
        try {
        int index=0; //area index
        for(AreaSnapshot area:stateSpace.get(stateIndex).areas) {
            int j=0;
            for(DEEvent object: area.eventQueue){
                NamedObj actor=(NamedObj)(object.actor());
                if(actor instanceof Track_R){
                        ((InsideDirector)((ControlArea)entities.get(index)).getDirector())._fillTrack(actor,area.trackActors.get(j));
                }
                if(actor instanceof Airport_R){
                    ((InsideDirector)((ControlArea)entities.get(index)).getDirector())._fillAirport(actor, area.airportActors.get(j));
                }
                if(actor instanceof DestinationAirport_R) {
                    ((InsideDirector)((ControlArea)entities.get(index)).getDirector())._fillDestinationAirport(actor, area.destinationAirportActors.get(j));
                }
                j++;
            }
            

            
            index++;
        }
        
        
        } catch (IllegalActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private boolean internalPostFire() throws IllegalActionException {
        // TODO Auto-generated method stub
        if(startTime>0 && (System.currentTimeMillis()-startTime)>=3600000) {
            timeOver=true;
            return false;
        }
        if(deadlockDetected==true)
        {
            return false;
        }

        if(!_internalTimedStates.isEmpty())
            _internalTimedStates.remove(0); // It may be empty, because all fired composite actors do not have
        //sequential events.
        if(!_internalTimedStates.isEmpty()) {
            
            
          //This is done to diagnose when we are passing from root of the parallel execution up.
            int tempIndex=internalTimedStatesMap.get(_internalTimedStates.get(0));
            //

            currentStateIndex=tempIndex;
           _changeState(currentStateIndex);
            _storeEvents(currentStateIndex);
            try {
                _internalEventQueue.clear();
                for(DEEvent event:stateSpace.get(currentStateIndex).parallelEventQueue)
                    _internalEventQueue.put(event);
                _fillModelInternalFire(stateSpace.get(currentStateIndex));
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            stateSpace.get(stateSpace.get(currentStateIndex).parent.get(0)).currentCArea=-1;

            return true;
        }
        return false;
    }

 

 
    /**
     * This function works same as the fire function. We assumed that to generate
     * the state space of the parallel part of the component, we have a transparent
     * new director which have fire and post fire. 
     * @throws IllegalActionException 
     */
    void internalFire() throws IllegalActionException {
        ArrayList<Integer[]> forkNodes=new ArrayList<Integer[]>(); 
        tempStates=new HashMap<>();
        ArrayList<Integer> forkOfIntBoundaries=new ArrayList<Integer>();
        //if the parallel event queue is empty: the model has been filled in internalPostfire()
        //Check if this state is a wrong state
        //-----------------------------------
        if(_internalEventQueue.isEmpty()) {
            for(Entry<Integer,info> entry:dependentActors.entrySet()) {
                if(entry.getValue().beenOcc==false && entry.getValue().beenReq==false)
                {
                    //if the message of an internal boundary actor has been rejected and 
                    // the destination pre-boundary actor has not been requested or occupied, 
                    // this trace is invalid and should be pruned. if the internal boundary actor has been 
                    //rejected it contains its message.
                    int entityNum=entityNumber(entry.getValue().trackId, 0);
                    if(((InsideDirector)((ControlArea)entities.get(entityNum)).getDirector()).tracklists.get(entry.getValue().trackId)._inTransit!=null) {
                        statesHasToRemoved.put(currentStateIndex, true);
                        _cleanModelInternalFire(currentStateIndex);
                        _removeStatesOfSeqUptoRejection(stateSpace.get(currentStateIndex).parent.get(0), entityNum, entry.getValue().trackId);
                        
                        _internalStopFireRequested = false;
                        return;
                    }
                }
             }
            
            // the state is not wrong, but it has to be added to real time states
                _cleanModelInternalFire(currentStateIndex);
                GlobalSnapshot temp=_internalTimedStates.get(0);
                int index=internalTimedStatesMap.get(temp);
                internalTimedStatesMap.remove(temp);
                
              //Add first event of each internal queue to the top-level queue
                for(int j=0;j<temp.areas.size();j++) {
                    AreaSnapshot area=temp.areas.get(j);
                    if(!area.eventQueue.isEmpty()) {
                        DEEvent e=area.eventQueue.get(0);
                        Actor a=(Actor) ((ControlArea)e.actor().getContainer());
                        
                        Time result = e.timeStamp();
                        if (result.compareTo(getModelTime()) < 0) {
                            result = getModelTime();
                        }
                        DEEvent event=new DEEvent(a, result, 0, 0);
                       _internalTimedStates.get(0).eventQueue.add(event); //we do not need to add this state to
                       //internalTimedStatesMap because the next phase is post fire in which the arrays are removed
                    }
                    
                }
               
                
                temp=_internalTimedStates.get(0); //We do not need to add 
                stateSpace.set(index, temp);
              
                
                _timeReachableStates.add(index, temp._modelTime);
                _timeReachableStatesMap.put(temp, index);
                
                _numberOfTimedStates++;
                
                _internalStopFireRequested = false;
                return;
        }
      //----------------------------------
        
        while (true) {  
            int result = _internalFire();
            if(deadlockDetected==true)
                return;
            assert result <= 1 && result >= -1;
            if (result == 1) {
                continue;
            } else if (result == -1) {
//                _noActorToFire();
                return;
            } // else if 0, keep executing

            if (_recentlyStVisited!=-1 || !_internalCheckForNextEvent()) {
                boolean flag=false;
//                int parentIndex=-1;
                
                for(Entry<Integer,info> entry:dependentActors.entrySet()) {
                   if(entry.getValue().beenOcc==false && entry.getValue().beenReq==false)
                   {
                       //if the message of an internal boundary actor has been rejected and 
                       // the destination pre-boundary actor has not been requested or occupied, 
                       // this trace is invalid and should be pruned. if the internal boundary actor has been 
                       //rejected it contains its message.
                       int entityNum=entityNumber(entry.getValue().trackId, 0);
                       if(((InsideDirector)((ControlArea)entities.get(entityNum)).getDirector()).tracklists.get(entry.getValue().trackId)._inTransit!=null) {
                           _removeStatesUptoRejection(currentStateIndex,entityNum,entry.getValue().trackId);
                           _internalStopFireRequested = false;
                           return;
                           
                       }
                   }
                }
                if(_recentlyStVisited==-1) {
                    // If this variable is not -1, then we are visiting an state twice
                    // in tempState and have to remove it from stateSpace. Otherwise the state
                    // is a time state.
 
                    int controlArea=stateSpace.get(stateSpace.get(currentStateIndex).parent.get(0)).currentCArea;
                    currentStateIndex=_createRealTimedStates(currentStateIndex);
                    
                    //We have changed the parent's control area in createRealTime, make it back.
                    stateSpace.get(stateSpace.get(currentStateIndex).parent.get(0)).currentCArea=controlArea;
                }
                
                do{

                    _cleanModelInternalFire(currentStateIndex);
                    currentStateIndex=stateSpace.get(currentStateIndex).parent.get(0);
                   
                  //If upToThisTaken of currentStateIndex refers to an internal boundary actor who want to send to a pre-boundary actor
                    int upToThis=stateSpace.get(currentStateIndex).upToThisTaken;
                    if(!forkOfIntBoundaries.isEmpty() && forkOfIntBoundaries.get(forkOfIntBoundaries.size()-1)==currentStateIndex)
                    {
                        forkOfIntBoundaries.remove(forkOfIntBoundaries.size()-1);
                    }
                    else {
                    if(upToThis!=-1)
                        if(stateSpace.get(currentStateIndex).parallelEventQueue.get(upToThis).ioPort()==null)
                            if(stateSpace.get(currentStateIndex).parallelEventQueue.get(upToThis).actor() instanceof Track_R) {
                                // if the target is the pre-boundary in the array of preToInternalB
                                Track_R actor=(Track_R)stateSpace.get(currentStateIndex).parallelEventQueue.get(upToThis).actor();
                                if(preToInternalB.containsKey(actor.destActor)) {

                                            isRejectPhase=true;
                                            forkOfIntBoundaries.add(currentStateIndex);

                                }
                            }
                    }
                    
                    if(!forkNodes.isEmpty() && currentStateIndex<forkNodes.get(forkNodes.size()-1)[0])
                    {
                        // We have passed the node in forkCounter and reached its parent
                        if(_recentlyStVisited!=-1) {
                            forkNodes.get(forkNodes.size()-1)[1]--;
                            int t=_recentlyStVisited;
                            while(forkNodes.get(forkNodes.size()-1)[0]!=t)
                            {
                                int parent=stateSpace.get(t).parent.get(0);
                                stateSpace.remove(t);
                                t=parent;
                                
                            }
                            _recentlyStVisited=-1;
                        }
                        if(forkNodes.get(forkNodes.size()-1)[1]==0)
                        {
                            //No child remains for this node and this node has to be removed.
                            _recentlyStVisited=forkNodes.get(forkNodes.size()-1)[0];
                            forkNodes.remove(forkNodes.size()-1);
                        }
                    }
                    if(stateSpace.get(currentStateIndex).currentCArea==-1) //root
                    {
                        flag=true;
                        forkNodes.clear();
                        tempStates.clear();
                        break;
                    }
                } while(isRejectPhase==false && !_internalCheckForNextEventInParent());
                 if(flag){
                    break;
                }
                
                if(!forkNodes.isEmpty() && forkNodes.get(forkNodes.size()-1)[0]==currentStateIndex) //We've already added this fork  node
                {
                    if(_recentlyStVisited==-1){
                        Integer [] a={forkNodes.get(forkNodes.size()-1)[0],forkNodes.get(forkNodes.size()-1)[1]+1};
                        forkNodes.remove(forkNodes.size()-1);
                        forkNodes.add(a);
                    }
                    else{
                        int t=_recentlyStVisited;
                        while(currentStateIndex!=t)
                        {
                            int parent=stateSpace.get(t).parent.get(0);
                            stateSpace.remove(t);
                            t=parent;
                            
                        }
                        _recentlyStVisited=-1;
                    }
                    
                    
                }
                else{
                    if(_recentlyStVisited==-1){
                        Integer [] a={currentStateIndex,2};
                        forkNodes.add(a);
                    }
                    else{
                        Integer [] a={currentStateIndex,1};
                        forkNodes.add(a);
                        int t=_recentlyStVisited;
                        while(currentStateIndex!=t)
                        {
                            int parent=stateSpace.get(t).parent.get(0);
                            stateSpace.remove(t);
                            t=parent;
                            
                        }
                        _recentlyStVisited=-1;
                        
                    }
                }
                //Fill _eventQueue
                _internalEventQueue.clear();
                
                if(isRejectPhase==false) 
                    for(int i=stateSpace.get(currentStateIndex).upToThisTaken+1;i<stateSpace.get(currentStateIndex).parallelEventQueue.size();i++)
                        _internalEventQueue.put(stateSpace.get(currentStateIndex).parallelEventQueue.get(i));
                else
                    for(int i=stateSpace.get(currentStateIndex).upToThisTaken;i<stateSpace.get(currentStateIndex).parallelEventQueue.size();i++)
                        _internalEventQueue.put(stateSpace.get(currentStateIndex).parallelEventQueue.get(i));
                
//                for(int i=stateSpace.get(currentStateIndex).upToThisTaken+1;i<stateSpace.get(currentStateIndex).parallelEventQueue.size();i++)
//                    _internalEventQueue.put(stateSpace.get(currentStateIndex).parallelEventQueue.get(i));
                // Fill MODEL
                try {
                    _fillModelInternalFire(stateSpace.get(currentStateIndex));
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } // else keep executing in the current iteration
        } // Close the BIG while loop.
        // Since we are now actually stopping the firing, we can set this false.
        _internalStopFireRequested = false;
    }
    
    private void _removeStatesUptoRejection(int stateIndex, int entityNum, int trackId) {
        // TODO Auto-generated method stub
        //we are traversing a trace in which the message of the internal boundary actor has been rejected.
        //first remove state in the parallel part up to the final node in the sequential part

       
        int t=stateIndex;
        while(stateSpace.get(stateSpace.get(t).parent.get(0)).currentCArea!=-1) {
            int parent=stateSpace.get(t).parent.get(0);
            try {
                _cleanModelInternalFire(t);
            } catch (IllegalActionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            stateSpace.remove(t);
            t=parent;
        }
        
        // now t shows the final state in the sequential part.
        // we should go up until we reach to the source of rejection.
        int sourceId=trackId;
        try {
        while(true) {
          //  int counter=0;
            for(DEEvent event: stateSpace.get(t).areas.get(entityNum).eventQueue) {
                //??? I changed the following line, check its correctness
                if(event.actor() instanceof Track_R)
                    if(((IntToken)((Track_R)event.actor()).trackId.getToken()).intValue()==sourceId && event.timeStamp().equals(getModelTime()))
                        return;
                    else
                        if(((IntToken)((Track_R)event.actor()).trackId.getToken()).intValue()==sourceId)
                        {
                            break;
                        }
         //       counter++;
            }
            statesHasToRemoved.put(t, true);
            t=stateSpace.get(t).parent.get(0);
            
        }
        } catch (IllegalActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    private void _removeStatesOfSeqUptoRejection(int stateIndex, int entityNum, int trackId) {
        // TODO Auto-generated method stub
        //we are traversing a trace in which the message of the internal boundary actor has been rejected.
        //Start from the node in the sequential part

        
        // stateIndex shows the prior state to the final state in the sequential part.
        // we should go up until we reach to the source of rejection.
        int sourceId=trackId;
        int t=stateIndex;
        try {
        while(true) {
          
            for(DEEvent event: stateSpace.get(t).areas.get(entityNum).eventQueue) {
               
                if(event.actor() instanceof Track_R)
                    if(((IntToken)((Track_R)event.actor()).trackId.getToken()).intValue()==sourceId && event.timeStamp().equals(getModelTime()))
                        return;
                    else
                        if(((IntToken)((Track_R)event.actor()).trackId.getToken()).intValue()==sourceId)
                        {
                            break;
                        }
    
            }
            statesHasToRemoved.put(t, true);
            t=stateSpace.get(t).parent.get(0);
            
        }
        } catch (IllegalActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
    /**
     * This function works on internalStates when we are going to execute the actors in parallel. It changes how
     * the actors are stored in a state.
     * @param stateIndex
     */
    private void _changeState(int stateIndex) {
        // First information of the actors referred to by the events in the event queue of each area are stored.
        // Then the actors referred to by the  parallel events are stored in each area.
        GlobalSnapshot temp= new GlobalSnapshot(stateSpace.get(stateIndex).areas, stateSpace.get(stateIndex).currentCArea, stateSpace.get(stateIndex).parent, stateSpace.get(stateIndex)._modelTime, stateSpace.get(stateIndex)._microstep, stateSpace.get(stateIndex).eventQueue, stateSpace.get(stateIndex).parallelEventQueue);
      //  GlobalSnapshot temp2= new GlobalSnapshot(stateSpace.get(stateIndex).areas, stateSpace.get(stateIndex).currentCArea, stateSpace.get(stateIndex).parent, stateSpace.get(stateIndex)._modelTime, stateSpace.get(stateIndex)._microstep, stateSpace.get(stateIndex).eventQueue, stateSpace.get(stateIndex).parallelEventQueue);
        
        
        int totalEvents=0;
        int indexOnParallel=0;
        
        for(AreaSnapshot area:temp.areas)
            totalEvents+=area.eventQueue.size();
        
        for(int i=0;i<order.size();i++) {
            //we first obtain that this area has how many parallel events.
            //parallel events are only stored in track actors.
            HashMap<Integer,TrackFields> trackFields=new HashMap<>();
            HashMap<Integer,AirportFeilds> airportFields=new HashMap<>();
            HashMap<Integer,DestinationAirportFields> destAirportFields=new HashMap<>();
            int numberOfParallel=stateSpace.get(stateIndex).areas.get(order.get(i)).trackActors.size()+stateSpace.get(stateIndex).areas.get(order.get(i)).destinationAirportActors.size()+stateSpace.get(stateIndex).areas.get(order.get(i)).airportActors.size()-stateSpace.get(stateIndex).areas.get(order.get(i)).eventQueue.size();
            int index=stateSpace.get(stateIndex).areas.get(order.get(i)).eventQueue.size();
            int k=0;
            for(int j=indexOnParallel;j<indexOnParallel+numberOfParallel;j++) {
                if(stateSpace.get(stateIndex).parallelEventQueue.get(j).actor() instanceof Track_R)
                {
                    
                    TrackFields x=new TrackFields(stateSpace.get(stateIndex).areas.get(order.get(i)).trackActors.get(index+k).called, stateSpace.get(stateIndex).areas.get(order.get(i)).trackActors.get(index+k).inTransit, stateSpace.get(stateIndex).areas.get(order.get(i)).trackActors.get(index+k).OutRoute, stateSpace.get(stateIndex).areas.get(order.get(i)).trackActors.get(index+k).transitExpires);
                 
                    temp.areas.get(order.get(i)).trackActors.remove(index+k);
                    trackFields.put(totalEvents, new TrackFields(x.called, x.inTransit, x.OutRoute, x.transitExpires));
                }
                else if(stateSpace.get(stateIndex).parallelEventQueue.get(j).actor() instanceof Airport_R) {
                    AirportFeilds x=new AirportFeilds(stateSpace.get(stateIndex).areas.get(order.get(i)).airportActors.get(index+k)._airplanes, stateSpace.get(stateIndex).areas.get(order.get(i)).airportActors.get(index+k)._inTransit, stateSpace.get(stateIndex).areas.get(order.get(i)).airportActors.get(index+k)._transitExpires);
                   
                    temp.areas.get(order.get(i)).airportActors.remove(index+k);
                    airportFields.put(totalEvents, new AirportFeilds(x._airplanes, x._inTransit, x._transitExpires));
                }
                else if(stateSpace.get(stateIndex).parallelEventQueue.get(j).actor() instanceof DestinationAirport_R) {
                    DestinationAirportFields x=new DestinationAirportFields(stateSpace.get(stateIndex).areas.get(order.get(i)).destinationAirportActors.get(index+k)._inTransit, 
                            stateSpace.get(stateIndex).areas.get(order.get(i)).destinationAirportActors.get(index+k)._transitExpires, stateSpace.get(stateIndex).areas.get(order.get(i)).destinationAirportActors.get(index+k)._called);
                    temp.areas.get(order.get(i)).destinationAirportActors.remove(index+k);
                    destAirportFields.put(totalEvents, new DestinationAirportFields( x._inTransit, x._transitExpires,x._called));
                }
                k++;
                totalEvents++;
            }
            
            for(Entry<Integer,TrackFields> entry:trackFields.entrySet()) {
                temp.areas.get(order.get(i)).trackActors.put(entry.getKey(), new TrackFields(entry.getValue().called, entry.getValue().inTransit, entry.getValue().OutRoute, entry.getValue().transitExpires));
            }
            for(Entry<Integer,AirportFeilds> entry:airportFields.entrySet()) {
                temp.areas.get(order.get(i)).airportActors.put(entry.getKey(), new AirportFeilds(entry.getValue()._airplanes, entry.getValue()._inTransit, entry.getValue()._transitExpires));
            }
            for(Entry<Integer,DestinationAirportFields> entry:destAirportFields.entrySet()) {
                temp.areas.get(order.get(i)).destinationAirportActors.put(entry.getKey(), new DestinationAirportFields(entry.getValue()._inTransit, entry.getValue()._transitExpires,entry.getValue()._called));
            }
            indexOnParallel=indexOnParallel+numberOfParallel;
        }
        
        stateSpace.set(stateIndex, temp);
        
    }

    /**
     * This function is used to fill the model when executing actors in parallel
     * @param globalSnapshot
     * @throws IllegalActionException 
     * @throws IllegalAccessException 
     */
    private void _fillModelInternalFire(GlobalSnapshot globalSnapshot) throws IllegalActionException, IllegalAccessException {
        // TODO Auto-generated method stub
        
        //Top-Level Queue
        _eventQueue.clear();
        for(int i=0;i<globalSnapshot.eventQueue.size();i++)
            _eventQueue.put(globalSnapshot.eventQueue.get(i));

        //ParallelQueue and internal queues
        
        int j=0;
        int totalEvents=0;
        for(AreaSnapshot area:globalSnapshot.areas) {
            int i=0;
            ((InsideDirector)((ControlArea)entities.get(j)).getDirector()).setEventQueue(area.eventQueue);
            for(DEEvent e: area.eventQueue) {
                NamedObj actor=(NamedObj)(e.actor());
                
                IOPort port=e.ioPort();
                if(port!=null){
                    String name=e.actor().getFullName()+port.getFullName();
                    ((InsideDirector)((ControlArea)entities.get(j)).getDirector())._fillInputPort(e,globalSnapshot.areas.get(j).inputTokens.get(name));
                }
                
                if(actor instanceof Track_R){
                    ((InsideDirector)((ControlArea)entities.get(j)).getDirector())._fillTrack(actor,globalSnapshot.areas.get(j).trackActors.get(i));
                }
                if(actor instanceof Airport_R){
                    ((InsideDirector)((ControlArea)entities.get(j)).getDirector())._fillAirport(actor, globalSnapshot.areas.get(j).airportActors.get(i));
                }
                if(actor instanceof DestinationAirport_R) {
                    ((InsideDirector)((ControlArea)entities.get(j)).getDirector())._fillDestinationAirport(actor, globalSnapshot.areas.get(j).destinationAirportActors.get(i));
                }
                i++;
                totalEvents++;
            }
            j++;
        }
        
        for(DEEvent e: globalSnapshot.parallelEventQueue) {
            NamedObj actor=(NamedObj)(e.actor());
            
            int index=-1;
            if(actor instanceof Track_R) {
                index=entityNumber(((IntToken)((Track_R)actor).trackId.getToken()).intValue(),0);
            }
            else if(actor instanceof Airport_R) {
                index=entityNumber(((IntToken)((Airport_R)actor).airportId.getToken()).intValue(),1);
            }
            else if(actor instanceof DestinationAirport_R)
                index=entityNumber(((IntToken)((DestinationAirport_R)actor).airportId.getToken()).intValue(),2);
            
            IOPort port=e.ioPort();
            if(port!=null){
                String name=e.actor().getFullName()+port.getFullName();
                ((InsideDirector)((ControlArea)entities.get(index)).getDirector())._fillInputPort(e,globalSnapshot.areas.get(index).inputTokens.get(name));
            }
            
            if(actor instanceof Track_R){
                ((InsideDirector)((ControlArea)entities.get(index)).getDirector())._fillTrack(actor,globalSnapshot.areas.get(index).trackActors.get(totalEvents));
            }
            if(actor instanceof Airport_R){
                ((InsideDirector)((ControlArea)entities.get(index)).getDirector())._fillAirport(actor, globalSnapshot.areas.get(index).airportActors.get(totalEvents));
            }
            if(actor instanceof DestinationAirport_R) {

                ((InsideDirector)((ControlArea)entities.get(index)).getDirector())._fillDestinationAirport(actor, globalSnapshot.areas.get(index).destinationAirportActors.get(totalEvents));
            }
            totalEvents++;
        }
        
    }

    
    
    /**
     * This function is used when we execute the actors in parallel
     * @param currentStateIndex2
     * @throws IllegalActionException 
     */
    private void _cleanModelInternalFire(int StateIndex) throws IllegalActionException {
        // TODO Auto-generated method stub
        Object[] objectArray=stateSpace.get(StateIndex).parallelEventQueue.toArray();
        _internalEventQueue.clear();
        for(Object object:objectArray) {
            NamedObj actor=(NamedObj) ((DEEvent)object).actor();
            IOPort port= ((DEEvent)object).ioPort();
            if(port!=null){
                Receiver[][] receiver=port.getReceivers();
                for(int i=0;i<receiver.length;i++){
                    for(int j=0;j<receiver[i].length;j++)
                        receiver[i][j].clear();
                } 
            }
            
            if(actor instanceof Track_R){
                ((Track_R)actor).cleanTrack();
                int entityNum=entityNumber(((IntToken)(((Track_R)actor).trackId.getToken())).intValue(), 0);
                ((InsideDirector)(((ControlArea)entities.get(entityNum)).getDirector()))._inTransit.put(((IntToken)(((Track_R)actor).trackId.getToken())).intValue(), false);
            }
            if(actor instanceof Airport_R){
                ((Airport_R)actor)._airplanes.clear();
                ((Airport_R)actor)._inTransit=null;
            }
            if(actor instanceof DestinationAirport_R){
                ((DestinationAirport_R)actor).cleanDestinationAirport();
            }
            
        }
        
        int k=0;
        for(Object entity:entities) {
            
            ((InsideDirector)((ControlArea) entity).getDirector()).clearEventQueue();
            Object[] eventArray=stateSpace.get(StateIndex).areas.get(k).eventQueue.toArray();
            for(Object object: eventArray ){
                NamedObj actor=(NamedObj) ((DEEvent)object).actor();
                IOPort port= ((DEEvent)object).ioPort();
                if(port!=null){
                    Receiver[][] receiver=port.getReceivers();
                    for(int i=0;i<receiver.length;i++){
                        for(int j=0;j<receiver[i].length;j++)
                            receiver[i][j].clear();
                    } 
                }
                if(actor instanceof Track_R){
                    ((Track_R)actor).cleanTrack();
                    ((InsideDirector)(((ControlArea)entity).getDirector()))._inTransit.put(((IntToken)(((Track_R)actor).trackId.getToken())).intValue(), false);
                }
                if(actor instanceof Airport_R){
                    ((Airport_R)actor)._airplanes.clear();
                    ((Airport_R)actor)._inTransit=null;
                }
                if(actor instanceof DestinationAirport_R){
                    ((DestinationAirport_R)actor).cleanDestinationAirport();
                }

            }
          k++;  
         }
        
    }

    /**
     * In this method the final timed states in the top level director are created. In other words,
     * some global states are added to the timedReachableStates and timeReachableStatesMap. 
     * First the events in _eventQueue of the composite components (ControlArea) are executed. Then, 
     * the events which are in the chainT0 are added for each component and are executed. The simplest solution
     * now is to execute all of them in parallel. In other words,  we call the fire function of the component
     * with a parameter. If the parameter is true, means that the final states created for the component are
     * final timed states, also we do not need to create the chains.
     * It may reach several timed states, because of the concurrency inside of each component.
     * @param stateIndex
     * @return
     * @throws IllegalActionException 
     */
    private int _createRealTimedStates(int stateIndex) throws IllegalActionException {
        // TODO Auto-generated method stub
        for(Object entity:entities) {
           ((InsideDirector)(((ControlArea)entity).getDirector())).removedEvents.clear();

           ((InsideDirector)(((ControlArea)entity).getDirector())).typeOfFiring=true;
        }
        
        _tempInternalTimedStates=new ArrayList<>();
        _tempInternalTimedStatesMap=new HashMap<>();
        ArrayList<GlobalSnapshot> iTimedStates=new ArrayList<GlobalSnapshot>();
        HashMap<GlobalSnapshot, Integer> iTimedStatesMap=new HashMap<>();
        GlobalSnapshot temp=new GlobalSnapshot(stateSpace.get(stateIndex).areas, stateSpace.get(stateIndex).currentCArea, stateSpace.get(stateIndex).parent, stateSpace.get(stateIndex)._modelTime, stateSpace.get(stateIndex)._microstep, stateSpace.get(stateIndex).eventQueue, stateSpace.get(stateIndex).parallelEventQueue);
        iTimedStates.add(temp);
        iTimedStatesMap.put(temp, stateIndex);

        
        int j=0;
        for(Object entity:entities) {
            int k=0;
            //Execute each entity per each iTimedStates. When the execution of an entity finishes, 
            // the iTimedStates is cleared, and the new states are added to it. Then next entity is executed 
            // per each state in iTimedStates.
            while(k<iTimedStates.size()) {

                currentStateIndex=iTimedStatesMap.get(iTimedStates.get(k));
                
                stateSpace.get(stateSpace.get(currentStateIndex).parent.get(0)).currentCArea=-1;
                
                stateSpace.get(currentStateIndex).currentCArea=((IntToken)((ControlArea)entity).areaId.getToken()).intValue();
                ((InsideDirector)(((ControlArea)entity).getDirector())).fire(); // this fire creates
                // new states and adds them to the _tempInternalTimedStates and _tempInternalTimedStatesMap.
                ((InsideDirector)(((ControlArea)entity).getDirector())).typeOfFiring=false;
                k++;
            }
            if(_tempInternalTimedStates.size()==0)
            {
                j++;
                continue;
            }
            iTimedStates.clear();
            iTimedStatesMap.clear();
            
            for(Entry<GlobalSnapshot,Integer> entry:_tempInternalTimedStatesMap.entrySet()) {
                GlobalSnapshot temp2=new GlobalSnapshot(entry.getKey().areas, entry.getKey().currentCArea, entry.getKey().parent, entry.getKey()._modelTime,entry.getKey()._microstep, entry.getKey().eventQueue, 
                        entry.getKey().parallelEventQueue);
                
                iTimedStates.add(temp2);
                iTimedStatesMap.put(temp2, entry.getValue());
            }
            
            _tempInternalTimedStates.clear();
            _tempInternalTimedStatesMap.clear();

            j++;
        }

        //The states in _internalTimedStates are real timed states when there is no entity to be fired.
        for(int i=0;i<iTimedStates.size();i++) {
            int index=iTimedStatesMap.get(iTimedStates.get(i));
            iTimedStatesMap.remove(iTimedStates.get(i));
            
            //Add first event of each internal queue to the top-level queue
            for(j=0;j<iTimedStates.get(i).areas.size();j++) {
                AreaSnapshot area=iTimedStates.get(i).areas.get(j);
                if(!area.eventQueue.isEmpty()) {
                    DEEvent e=area.eventQueue.get(0);
                    Actor a=(Actor) ((ControlArea)e.actor().getContainer());
                    
                    Time result = e.timeStamp();
                    if (result.compareTo(getModelTime()) < 0) {
                        // NOTE: There is no assurance in a multithreaded situation
                        // that time will not advance prior to the posting of
                        // this returned time on the local event queue, so
                        // an exception can still occur reporting an attempt
                        // to post an event in the past.
                        result = getModelTime();
                    }
                    
                    DEEvent event=new DEEvent(a, result, 0, 0);
                    
                    iTimedStates.get(i).eventQueue.add(event);

                }
            }
            iTimedStatesMap.put(iTimedStates.get(i), index);
            
            GlobalSnapshot newSnapshot=new GlobalSnapshot(iTimedStates.get(i).areas, iTimedStates.get(i).currentCArea, iTimedStates.get(i).parent, iTimedStates.get(i)._modelTime, iTimedStates.get(i)._microstep, iTimedStates.get(i).eventQueue, iTimedStates.get(i).parallelEventQueue);
            stateSpace.set(index, newSnapshot);
            _timeReachableStates.add(index, iTimedStates.get(i)._modelTime);
//            _addToTimedStates(iTimedStates.get(i),index);
            _timeReachableStatesMap.put(newSnapshot, index);
            
            _numberOfTimedStates++;
            
        }
        return stateIndex; //because the current state index is changed but we need to reverse back on it in the internalFire function.
    }
    

   

    private boolean _internalCheckForNextEventInParent() throws IllegalActionException {
        // TODO Auto-generated method stub
        // The following code enforces that a firing of a
        // DE director only handles events with the same tag.
        // If the earliest event in the event queue is in the future,
        // this code terminates the current iteration.
        // This code is applied on both embedded and top-level directors.
//        Object[] objectArray=_stateSpace.get(_currentStateIndex).eventQueue.toArray();
        if(stateSpace.get(currentStateIndex).parallelEventQueue.size()!=0 && 
                stateSpace.get(currentStateIndex).upToThisTaken+1 < stateSpace.get(currentStateIndex).parallelEventQueue.size()) {
            DEEvent next=stateSpace.get(currentStateIndex).parallelEventQueue.get(stateSpace.get(currentStateIndex).upToThisTaken+1);
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

    /**
     * Check if the internalEventQueue has next event to be taken at the current time.
     * @return
     * @throws IllegalActionException 
     */
    private boolean _internalCheckForNextEvent() throws IllegalActionException {
        // TODO Auto-generated method stub
        // The following code enforces that a firing of a
        // DE director only handles events with the same tag.
        // If the earliest event in the event queue is in the future,
        // this code terminates the current iteration.
        // This code is applied on both embedded and top-level directors.
        synchronized (_eventQueueLock) {
            if (!_internalEventQueue.isEmpty()) {
                DEEvent next = _internalEventQueue.get();

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
            else{
                return false;
            }
        }
        return true;
    }

    private int _internalFire() throws IllegalActionException {
        // TODO Auto-generated method stub
        //When an actor is executed, you should looked at the chains, updates them 
        // and update internalEventQueue.. Also notice that the event queue of the components are changed.
        // also if the a boundary send happens something is changed.
        
        // Find the next actor to be fired.
        //MARYAM
        // This will be the state which is obtained by firing an actor.
        // The information of this state will be updated. 
        stateSpace.add(new GlobalSnapshot(-1, currentStateIndex, getModelTime(), _microstep, _eventQueue, null));
        _nextStateIndex=stateSpace.size()-1; 
        //
        Actor actorToFire = _internalGetNextActorToFire();

        if(actorToFire==null){
            stateSpace.remove(_nextStateIndex);
            return -1;
        }
        
        _numberOfAllStates++;
        
        if(actorToFire instanceof Track_R)
        if(((StringToken)((Track_R)actorToFire).isBoundary.getToken()).stringValue().equals("pb"))
            if(((Track_R)actorToFire)._inTransit!=null)
                updateOccInfoOfDependentActors(((IntToken)((Track_R)actorToFire).trackId.getToken()).intValue(), true);
        
        stateSpace.get(_nextStateIndex).name=actorToFire.getFullName();
        stateSpace.get(_nextStateIndex)._microstep=_microstep;
        if(actorToFire instanceof Track_R)
            stateSpace.get(_nextStateIndex).currentCArea=((IntToken)((ControlArea)entities.get(entityNumber(((IntToken)((Track_R) actorToFire).trackId.getToken()).intValue(), 0))).areaId.getToken()).intValue();
//  
        if(actorToFire instanceof Airport_R)
            stateSpace.get(_nextStateIndex).currentCArea=((IntToken)((ControlArea)entities.get(entityNumber(((IntToken)((Airport_R) actorToFire).airportId.getToken()).intValue(), 1))).areaId.getToken()).intValue();
//
        if(actorToFire instanceof DestinationAirport_R)
            stateSpace.get(_nextStateIndex).currentCArea=((IntToken)((ControlArea)entities.get(entityNumber(((IntToken)((DestinationAirport_R) actorToFire).airportId.getToken()).intValue(), 2))).areaId.getToken()).intValue();

        if (!actorToFire.prefire()) {
//            break;
        }
        actorToFire.fire();
        isRejectPhase=false;
        
        if (!actorToFire.postfire()) {
            // This actor requests not to be fired again.
            _disableActor(actorToFire);
//            break;
        }

        //MARYAM:
        //Update the _internalEventQueue of the _nextStateIndex
        
        int upToThis=stateSpace.get(currentStateIndex).upToThisTaken;
        
        for(int i=0;i<upToThis;i++)
            _internalEventQueue.put(stateSpace.get(currentStateIndex).parallelEventQueue.get(i));
        
        Object[] objectArray=_internalEventQueue.toArray();
        for(Object object: objectArray)
            stateSpace.get(_nextStateIndex).parallelEventQueue.add((DEEvent) object);
        
        
        _storeStateInternal(_nextStateIndex,currentStateIndex);
        currentStateIndex=_nextStateIndex;
        
        
        if(hasBeenAccepted==true && rejected==true && !boundaryTrack.currentTime.equals(getModelTime()) 
                && boundaryTrack._inTransit!=null && !boundaryTrack._transitExpires.equals(getModelTime())) {
            // If the send from internal boundary to the pre-boundary is accepted but from pre-boundary to the
            // boundary is rejected, and boundary actor has a token and will fire in the future to send it out,
            // all tracks who are dependent to the boundary actor are rejected.
            int entityNum=entityNumber(((IntToken)((Track_R)actorToFire).trackId.getToken()).intValue(),0);
            _removeStatesUptoAcceptance(currentStateIndex,entityNum,((IntToken)((Track_R)actorToFire).trackId.getToken()).intValue());
            hasBeenAccepted=false;
            rejected=false;
            boundaryTrack=null;
            return -1;
        }
        else if(hasBeenAccepted==true && rejected==true) {
         // If the send from internal boundary to the pre-boundary is accepted but from pre-boundary to the
            // boundary is rejected, do not continue and prune.
            _recentlyStVisited=currentStateIndex;
        }
        
        // Check whether the _currentStateIndex exists in tempStates
        if(tempStates.containsKey(stateSpace.get(currentStateIndex)))
            _recentlyStVisited=currentStateIndex;
        else if(_recentlyStVisited!=currentStateIndex)
            tempStates.put(stateSpace.get(currentStateIndex), currentStateIndex);
        
        hasBeenAccepted=false;
        rejected=false;
        boundaryTrack=null;
        
        return 0;
    }

    private void _removeStatesUptoAcceptance(int stateIndex, int entityNum, int trackId) {
        // TODO Auto-generated method stub
      //we are traversing a trace in which the message of the internal boundary actor has been accepted.
        //first remove state in the parallel part up to the final node in the sequential part
        
        int t=stateIndex;
        while(stateSpace.get(stateSpace.get(t).parent.get(0)).currentCArea!=-1) {
            int parent=stateSpace.get(t).parent.get(0);
            try {
                _cleanModelInternalFire(t);
            } catch (IllegalActionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            stateSpace.remove(t);
            t=parent;
        }
        
        // now t shows the final state in the sequential part.
        // we should go up until we reach to the source of acceptance.
        int sourceId=dependentActors.get(trackId).trackId;
        try {
        while(true) {
            int counter=0;
            for(DEEvent event: stateSpace.get(t).areas.get(entityNum).eventQueue) {
                if(event.actor() instanceof Track_R)
                    if(((IntToken)((Track_R)event.actor()).trackId.getToken()).intValue()==sourceId &&
                            stateSpace.get(t).areas.get(entityNum).trackActors.get(counter).inTransit!=null)
                        return;
                    else
                        if(((IntToken)((Track_R)event.actor()).trackId.getToken()).intValue()==sourceId)
                        {
                            break;
                        }
                counter++;
            }
            statesHasToRemoved.put(t, true);
            t=stateSpace.get(t).parent.get(0);
            
        }
        } catch (IllegalActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
    }

    /**
     * This function stores all chains of all composite actors,
     * the eventQueue of every component along with the states
     * and copies the top-level event queue.
     * @param _nextStateIndex2
     * @throws IllegalActionException 
     */
    private void _storeStateInternal(int stateIndex,int currenIndex) throws IllegalActionException {
        // TODO Auto-generated method stub
        
        stateSpace.get(stateIndex).eventQueue=stateSpace.get(currenIndex).eventQueue;
        int i=0;
        for(Object entity: entities) {
            InsideDirector _director=((InsideDirector)((ControlArea)entity).getDirector());
            //Add area, and eventQueue of each component
            stateSpace.get(stateIndex).areas.add(new AreaSnapshot(_director.getEventQueue(), null, getModelTime(), _microstep));
            i++;
        }
        
        //Store actors
        int j=0;
        int totalEvents=0;
        for(AreaSnapshot area: stateSpace.get(stateIndex).areas) {
            int eventCounter=0;
            for(DEEvent e: area.eventQueue) {
                NamedObj actor=(NamedObj) e.actor();
                IOPort port=e.ioPort();
                if(port!=null){
                    Receiver[][] receiver=port.getReceivers();
                    Map<Integer, Token> temp=new TreeMap<Integer, Token>();
                    //For each channel, check existence of the token in it. 
                    for(i=0;i<port.getWidth();i++){
                        if(port.hasNewToken(i)){
                            Token token=((ATCReceiver_R)receiver[i][0]).getToken();
                            temp.put(i,token);
                        }
                    }
                    // For one event, we check all channels of the port.
                        stateSpace.get(stateIndex).areas.get(j).inputTokens.put(actor.getFullName()+port.getFullName(), temp);
                }
                // End of storing tokens on the ports
                if(actor instanceof Airport_R){
                    stateSpace.get(stateIndex).areas.get(j).airportActors.put(eventCounter,new AirportFeilds(
                            ((Airport_R)actor)._airplanes,((Airport_R)actor)._inTransit, ((Airport_R)actor)._transitExpires));
                }
                else if(actor instanceof DestinationAirport_R) {
                    stateSpace.get(stateIndex).areas.get(j).destinationAirportActors.put(eventCounter, new DestinationAirportFields(((DestinationAirport_R)actor)._inTransit, ((DestinationAirport_R)actor)._transitExpires, ((DestinationAirport_R)actor)._called));
                }
                else if(actor instanceof  Track_R ){
                    stateSpace.get(stateIndex).areas.get(j).trackActors.put(eventCounter, new TrackFields(
                            ((Track_R)actor)._called, ((Track_R)actor)._inTransit, ((Track_R)actor)._OutRoute, ((Track_R)actor)._transitExpires));
                }
             eventCounter++;
             totalEvents++;
            }
            j++;
        }
        
        
        for(DEEvent object:stateSpace.get(stateIndex).parallelEventQueue) {
            NamedObj actor=(NamedObj) object.actor();
            
            int areaNum=-1;
            if(actor instanceof Track_R) {
                areaNum=entityNumber(((IntToken)((Track_R)actor).trackId.getToken()).intValue(),0);
            }
            else if(actor instanceof Airport_R) {
                areaNum=entityNumber(((IntToken)((Airport_R)actor).airportId.getToken()).intValue(),1);
            }
            else if(actor instanceof DestinationAirport_R)
                areaNum=entityNumber(((IntToken)((DestinationAirport_R)actor).airportId.getToken()).intValue(),2);
            
            
            IOPort port=object.ioPort();
            if(port!=null){
                Receiver[][] receiver=port.getReceivers();
                Map<Integer, Token> temp=new TreeMap<Integer, Token>();
                //For each channel, check existence of the token in it. 
                for(i=0;i<port.getWidth();i++){
                    if(port.hasNewToken(i)){
                        Token token=((ATCReceiver_R)receiver[i][0]).getToken();
                        temp.put(i,token);
                    }
                }
                // For one event, we check all channels of the port.
                    stateSpace.get(stateIndex).areas.get(areaNum).inputTokens.put(actor.getFullName()+port.getFullName(), temp);
            }
            // End of storing tokens on the ports
            
            if(actor instanceof Airport_R){
                stateSpace.get(stateIndex).areas.get(areaNum).airportActors.put(totalEvents,new AirportFeilds(
                        ((Airport_R)actor)._airplanes,((Airport_R)actor)._inTransit, ((Airport_R)actor)._transitExpires));
            }
            else if(actor instanceof DestinationAirport_R) {
                stateSpace.get(stateIndex).areas.get(areaNum).destinationAirportActors.put(totalEvents, new DestinationAirportFields(((DestinationAirport_R)actor)._inTransit, ((DestinationAirport_R)actor)._transitExpires, ((DestinationAirport_R)actor)._called));
            }
            else if(actor instanceof  Track_R ){
                stateSpace.get(stateIndex).areas.get(areaNum).trackActors.put(totalEvents, new TrackFields(
                        ((Track_R)actor)._called, ((Track_R)actor)._inTransit, ((Track_R)actor)._OutRoute, ((Track_R)actor)._transitExpires));
            }
            totalEvents++;
        }
        
    }


    private Actor _internalGetNextActorToFire() throws IllegalActionException {
        if (_internalEventQueue == null) {
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
                if (_internalEventQueue.isEmpty()) {
                    // If the event queue is empty,
                    // jump out of the loop: LOOPLABEL::GetNextEvent
                    break;
                }
                nextEvent = _internalEventQueue.get();


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
                    lastFoundEvent = _internalEventQueue.take();
                        IOPort port=((DEEvent)lastFoundEvent).ioPort();
                        if(port!=null) {
                            //Triggering event
                            hasBeenAccepted=true;
                        }
                    
                    currentTime = lastFoundEvent.timeStamp();
                    actorToFire = lastFoundEvent.actor();

                    // Advance the current time to the event time.
                    // NOTE: This is the only place that the model time changes.
                    if(isRejectPhase==false) {
                        // only increase uptothistaken of the parent, if we are in the accept phase.
                        stateSpace.get(currentStateIndex).upToThisTaken++;
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
                        DEEvent temp=_internalEventQueue.take();
                      //MARYAM
                        // Although this would not happen for us, because in our model
                        // every track actor is fired through only one event

                        stateSpace.get(currentStateIndex).upToThisTaken++;
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
    protected int _fire() throws IllegalActionException {
        // Find the next actor to be fired.
        //MARYAM:// Create initial state
//        if(currentStateIndex==313)
//            System.out.print("hey");
        if(currentStateIndex==-1)
        {
            currentStateIndex=0;
            stateSpace.add(new GlobalSnapshot(-1, -1, getModelTime(), _microstep, _eventQueue,null));
           
            _numberOfAllStates++;
            //It seems that we do not need to store at the first state.
//            for(int i=0;i<entities.size();i++)
//                _storeState(currentStateIndex,i);
            _timeReachableStates.add(currentStateIndex, getModelTime());
            _currentTimedState= stateSpace.get(currentStateIndex);
            _timeReachableStatesMap.put(_currentTimedState, currentStateIndex);
            for(int i=0;i<entities.size();i++)
                stateSpace.get(currentStateIndex).areas.add(new AreaSnapshot());
            
            _numberOfTimedStates++;
          
        }
        
        //MARYAM:
        //If we have a timed transition, we create a new state, also the selected event
        // is removed from the created state. Otherwise, we do not create a new state and 
        //remove the event from the current state. We do all of these in getNextActorToFire().
        
        Actor actorToFire = _getNextActorToFire();
        
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
//        if(actorToFire instanceof Airport)
//            if(((Airport)actorToFire)._airplanes.size()==0)
//            {
//                String k="";
//                for(int i=0;i<_stateSpace.size();i++)
//                    k+=_makeString(i, _stateSpace.get(i));
//                if(true)
//                    throw new IllegalActionException(k);
//            }

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
        return 0;
    }
    

    @Override
    protected Actor _getNextActorToFire() throws IllegalActionException {
        if (_eventQueue == null) {
            throw new IllegalActionException(
                    "Fire method called before the preinitialize method.");
        }
        //MARYAM
        boolean timeIncreased=false;
        //
        
        Actor actorToFire = null;
        DEEvent lastFoundEvent = null;
        DEEvent nextEvent = null;

        // Keep taking events out until there are no more events that have the
        // same tag and go to the same destination actor, or until the queue is
        // empty, or until a stop is requested.
        // LOOPLABEL::GetNextEvent
        while (!_stopRequested) {
            // Get the next event from the event queue.
            if (_stopWhenQueueIsEmpty) {
                if (_eventQueue.isEmpty()) {
                    // If the event queue is empty,
                    // jump out of the loop: LOOPLABEL::GetNextEvent
                    break;
                }
            }

            if (isEmbedded()) {
                // If the director is not at the top level.
                if (_eventQueue.isEmpty()) {
                    // This could happen if the container simply fires
                    // this composite at times it chooses. Most directors
                    // do this (SDF, SR, Continuous, etc.). It can also
                    // happen if an input is provided to a parameter port
                    // and the container is DE.
                    // In all these cases, no actors inside need to be
                    // fired.
                    break;
                }
                // For an embedded DE director, the following code prevents
                // the director from reacting to future events with bigger
                // time values in their tags.
                // For a top-level DE director, there is no such constraint
                // because the top-level director is responsible to advance
                // simulation by increasing the model tag.
                nextEvent = _eventQueue.get();

                // An embedded director should process events
                // that only happen at the current tag.
                // If the event is in the past, that is an error,
                // because the event should have been consumed in prefire().
                if (nextEvent.timeStamp().compareTo(getModelTime()) < 0) {
                    // missed an event
                    throw new IllegalActionException(
                            "Fire: Missed an event: the next event tag "
                                    + nextEvent.timeStamp() + " :: "
                                    + nextEvent.microstep()
                                    + " is earlier than the current model tag "
                                    + getModelTime() + " :: " + _microstep
                                    + " !");
                }

                // If the event is in the future time, it is ignored
                // and will be processed later. There is some complexity
                // here for backward compatibility with directors that do
                // not support superdense time. If the enclosing director
                // does not support superdense time, then we ignore the
                // microstep. Otherwise, we require the microstep of
                // the event to match the microstep that was set in
                // prefire(), which matches the microstep of the enclosing
                // director.
                boolean microstepMatches = true;
                Nameable container = getContainer();
                if (container instanceof CompositeActor) {
                    Director executiveDirector = ((CompositeActor) container)
                            .getExecutiveDirector();
                    // Some composites, such as RunCompositeActor want to be treated
                    // as if they are at the top level even though they have an executive
                    // director, so be sure to check _isTopLevel().
                    if (executiveDirector instanceof SuperdenseTimeDirector
                            && !_isTopLevel()) {
                        // If the next event microstep in the past (which it should
                        // not be normally), then we will consider it to match.
                        microstepMatches = nextEvent.microstep() <= _microstep;
                    }
                }

                int comparison = nextEvent.timeStamp()
                        .compareTo(getModelTime());
                //****************Changed by Maryam
               // if (comparison > 0 || comparison == 0 && !microstepMatches) { ///????????????????
                if (comparison > 0) {
                    // reset the next event
                    nextEvent = null;

                    // jump out of the loop: LOOPLABEL::GetNextEvent
                    break;
                }
            } else { // if (!topLevel)
                // If the director is at the top level
                // If the event queue is empty, normally
                // a blocking read is performed on the queue.
                // However, there are two conditions that the blocking
                // read is not performed, which are checked below.
                if (_eventQueue.isEmpty()) {
                    // The two conditions are:
                    // 1. An actor to be fired has been found; or
                    // 2. There are no more events in the event queue,
                    // and the current time is equal to the stop time.
                    if (actorToFire != null
                            || getModelTime().equals(getModelStopTime())) {
                        // jump out of the loop: LOOPLABEL::GetNextEvent
                        break;
                    }
                }

                // Otherwise, if the event queue is empty,
                // a blocking read is performed on the queue.
                // stopFire() needs to also cause this to fall out!
                synchronized (_eventQueueLock) {
                    while (_eventQueue.isEmpty() && !_stopRequested
                            && !_stopFireRequested) {
                        if (_debugging) {
                            _debug("Queue is empty. Waiting for input events.");
                        }

                        try {
                            // NOTE: Release the read access held
                            // by this thread to prevent deadlocks.
                            // NOTE: If a ChangeRequest has been requested,
                            // then _eventQueue.notifyAll() is called
                            // and stopFire() is called, so we will stop
                            // waiting for events. However,
                            // CompositeActor used to call stopFire() before
                            // queuing the change request, which created the risk
                            // that the below wait() would be terminated by
                            // a notifyAll() on _eventQueue with _stopFireRequested
                            // having been set, but before the change request has
                            // actually been filed.  See CompositeActor.requestChange().
                            // Does this matter? It means that on the next invocation
                            // of the fire() method, we could resume waiting on an empty queue
                            // without having filed the change request. That filing will
                            // no longer succeed in interrupting this wait, since
                            // stopFire() has already been called. Only on the next
                            // instance of change request would the first change
                            // request get a chance to execute.
                            workspace().wait(_eventQueueLock);
                        } catch (InterruptedException e) {
                            // If the wait is interrupted,
                            // then stop waiting.
                            break;
                        }
                    } // Close the blocking read while loop

                    // To reach this point, either the event queue is not empty,
                    // or _stopRequested or _stopFireRequested is true, or an interrupted exception
                    // happened.
                    if (_eventQueue.isEmpty()) {
                        // Stop is requested or this method is interrupted.
                        // This can occur, for example, if a change has been requested.
                        // jump out of the loop: LOOPLABEL::GetNextEvent
                        return null;
                    }
                    // At least one event is found in the event queue.
                    nextEvent = _eventQueue.get();
                } // Close synchronized block
            }

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
                int depth = 0;
                try {
                    synchronized (_eventQueueLock) {
                        lastFoundEvent = _eventQueue.get();
                        currentTime = _consultTimeRegulators(
                                lastFoundEvent.timeStamp());

                        // NOTE: Synchronize to real time here for backward compatibility,
                        // but the preferred way to do this is now to use a
                        // {@link SynchronizeToRealTime} attribute, which implements the
                        //  {@link TimeRegulator} interface.
                        if (_synchronizeToRealTime) {
                            // If synchronized to the real time.
                            Manager manager = ((CompositeActor) getContainer())
                                    .getManager();
                            while (!_stopRequested && !_stopFireRequested) {
                                lastFoundEvent = _eventQueue.get();
                                currentTime = lastFoundEvent.timeStamp();

                                if (currentTime
                                        .compareTo(getModelStopTime()) > 0) {
                                    // Next event is past the stop time of the model.
                                    // Do not stall.
                                    break;
                                }

                                long elapsedTime = elapsedTimeSinceStart();

                                // NOTE: We assume that the elapsed time can be
                                // safely cast to a double.  This means that
                                // the DE domain has an upper limit on running
                                // time of Double.MAX_VALUE milliseconds.
                                double elapsedTimeInSeconds = elapsedTime
                                        / 1000.0;
                                ptolemy.actor.util.Time elapsed = new ptolemy.actor.util.Time(
                                        this, elapsedTimeInSeconds);
                                if (currentTime.compareTo(elapsed) <= 0) {
                                    // Enough real time has passed already. Do not stall.
                                    break;
                                }

                                // NOTE: We used to do the following, but it had a limitation.
                                // In particular, if any user code also calculated the elapsed
                                // time and then constructed a Time object to post an event
                                // on the event queue, there was no assurance that the quantization
                                // would be the same, and hence it was possible for that event
                                // to be in the past when posted, even if done in the same thread.
                                // To ensure that the comparison of current time against model time
                                // always yields the same result, we have to do the comparison using
                                // the Time class, which is what the event queue does.
                                /*
                                if (currentTime.getDoubleValue() <= elapsedTimeInSeconds) {
                                    break;
                                }*/

                                long timeToWait = (long) (currentTime
                                        .subtract(elapsed).getDoubleValue()
                                        * 1000.0);

                                if (timeToWait > 0) {
                                    if (_debugging) {
                                        _debug("Waiting for real time to pass: "
                                                + timeToWait);
                                    }

                                    try {
                                        // NOTE: The built-in Java wait() method
                                        // does not release the
                                        // locks on the workspace, which would block
                                        // UI interactions and may cause deadlocks.
                                        // SOLUTION: explicitly release read permissions.
                                        depth = _workspace
                                                .releaseReadPermission();
                                        // Allow change requests to execute immediately while we are waiting.
                                        // This will have the side effect of executing any pending change requests.
                                        setDeferringChangeRequests(false);
                                        // Tell the manager what thread is waiting.
                                        manager.setWaitingThread(
                                                Thread.currentThread());
                                        _eventQueueLock.wait(timeToWait);
                                    } catch (InterruptedException ex) {
                                        // Ignore and circulate around the loop.
                                        // The interrupt could be due to a change request,
                                        // which we will want to process.
                                        // This used to do the following with flawed reasoning:
                                        /*
                                        throw new IllegalActionException(
                                            this,
                                            ex,
                                            "Thread interrupted when waiting for"
                                                    + " real time to match model time.");
                                        */
                                        // The reasoning was:
                                        // Continue executing?
                                        // No, because this could be a problem if any
                                        // actor assumes that model time always exceeds
                                        // real time when synchronizeToRealTime is set.
                                        //
                                        // But this is flawed because we are in a while loop
                                        // that will check again for matching to real time.
                                        // EAL 10/27/15.
                                    } finally {
                                        setDeferringChangeRequests(true);
                                        manager.setWaitingThread(null);
                                    }
                                }
                            } // while
                              // If stopFire() has been called, then the wait for real
                              // time above was interrupted by a change request. Hence,
                              // real time will not have reached the time of the first
                              // event in the event queue. If we allow this method to
                              // proceed, it will set model time to that event time,
                              // which is in the future. This violates the principle
                              // of synchronize to real time.  Hence, we must return
                              // without processing the event or incrementing time.

                            // NOTE: CompositeActor used to call stopFire() before
                            // queuing the change request, which created the risk
                            // that the above wait() would be terminated by
                            // a notifyAll() on _eventQueue with _stopFireRequested
                            // having been set, but before the change request has
                            // actually been filed.  See CompositeActor.requestChange().
                            // Does this matter? It means that on the next invocation
                            // of the fire() method, we could resume processing the
                            // same event, waiting for real time to elapse, without
                            // having filed the change request. That filing will
                            // no longer succeed in interrupting this wait, since
                            // stopFire() has already been called. Alternatively,
                            // before we get to the wait for real time in the next
                            // firing, the change request could complete and be
                            // executed.
                            if (_stopRequested || _stopFireRequested) {
                                return null;
                            }
                        } // if (_synchronizeToRealTime)
                    } // sync
                } finally {
                    if (depth > 0) {
                        _workspace.reacquireReadPermission(depth);
                    }
                }

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

                    // NOTE: The _enqueueEvent method discards the events
                    // for disabled actors.
                    if (_disabledActors != null
                            && _disabledActors.contains(actorToFire)) {
                        // This actor has requested not to be fired again.
                        if (_debugging) {
                            _debug("Skipping disabled actor: ",
                                    ((Nameable) actorToFire).getFullName());
                        }

                        actorToFire = null;

                        // start a new iteration of the loop:
                        // LOOPLABEL::GetNextEvent
                        continue;
                    }

                    // Advance the current time to the event time.
                    // NOTE: This is the only place that the model time changes.
                    
                    //*************Added by Maryam
                    if(currentTime.compareTo(getModelTime())>0)
                    {
                        // We have a timed transition
                        timeIncreased=true;
                    }
                    //*************
                    
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
                if (nextEvent.timeStamp().equals(lastFoundEvent.timeStamp())
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
                    }
                } else {
                    // Next event has a future tag or a different destination.
                    break;
                }
            }
            if (actorToFire != null && _aspectsPresent) {
                if (_actorsFinished.contains(actorToFire)) {
                    _actorsFinished.remove(actorToFire);
                } else if (!_schedule((NamedObj) actorToFire, getModelTime())) {
                    _nextScheduleTime.get(_aspectForActor.get(actorToFire))
                            .add(getModelTime());
                    if (_actorsInExecution == null) {
                        _actorsInExecution = new HashMap();
                    }
                    List<DEEvent> events = _actorsInExecution.get(actorToFire);
                    if (events == null) {
                        events = new ArrayList<DEEvent>();
                    }
                    events.add(lastFoundEvent);
                    _actorsInExecution.put(actorToFire, events);
                    actorToFire = null;
                }
            }
        } // close the loop: LOOPLABEL::GetNextEvent

        //MARYAM:
        if(actorToFire!=null) {
            currentUderAnalysisRegion=((IntToken)((ControlArea)actorToFire).areaId.getToken()).intValue();
            if(timeIncreased==true)
            {
                // We have a timed transition
                stateSpace.add(new GlobalSnapshot(currentUderAnalysisRegion, currentStateIndex, getModelTime(), _microstep, _eventQueue,null));
                // Change the parent's control area to -1
               // if(stateSpace.get(currentStateIndex).parent!=null && stateSpace.get(currentStateIndex).parent.get(0)!=-1)
                currentStateIndex=stateSpace.size()-1;
               stateInAfterTime=currentStateIndex;
                stateSpace.get(stateSpace.get(currentStateIndex).parent.get(0)).currentCArea=-1;
                _storeStateTop(currentStateIndex);
                
                _numberOfAllStates++;
            }
            else
            {
               stateSpace.get(currentStateIndex).currentCArea=currentUderAnalysisRegion;
               stateSpace.get(currentStateIndex).eventQueue.clear();
               Object[] objectArray=_eventQueue.toArray();
               for(Object object: objectArray)
                   stateSpace.get(currentStateIndex).eventQueue.add((DEEvent)object);
               //_storeStateTop(currentUderAnalysisRegion,currentStateIndex); //I guess we already have this information in the state
            }
        }
        //
        
        // Note that the actor to be fired can be null.
        return actorToFire;
    }

    
    
    
    /**
     * This function is used to store the state when a composite actor is selected to be fired.
     * In this function, only we need to store the event queue of the composite component
     * along with its actors, and also the chains of other components.
     * @param stateIndex
     * @param currentArea
     * @throws IllegalActionException 
     */
    private void _storeStateTop(int currentArea, int stateIndex) throws IllegalActionException {
        // TODO Auto-generated method stub
        
        int i=0;
        if(stateIndex==27)
            stateIndex=27;
        
        for(Object entity: entities) {
            int eventCounter=0;
            InsideDirector _director=((InsideDirector)((ControlArea)entity).getDirector());
            //Add area, and eventQueue of each component
            if(((IntToken)((ControlArea)entity).areaId.getToken()).intValue()!=currentArea) {
                stateSpace.get(stateIndex).areas.add(new AreaSnapshot(_director.getEventQueue(), null, getModelTime(), _microstep));
            }


            for(Object e: _director.getEventQueue().toArray()) {
                    stateSpace.get(stateIndex).areas.get(i).eventQueue.add((DEEvent)e);
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
                        stateSpace.get(stateIndex).areas.get(i).inputTokens.put(actor.getFullName()+port.getFullName(), temp);
                }
                // End of storing tokens on the ports
                if(actor instanceof Airport_R){
                    stateSpace.get(stateIndex).areas.get(i).airportActors.put(eventCounter,new AirportFeilds(
                            ((Airport_R)actor)._airplanes,((Airport_R)actor)._inTransit, ((Airport_R)actor)._transitExpires));
                }
                else if(actor instanceof DestinationAirport_R) {
                    stateSpace.get(stateIndex).areas.get(i).destinationAirportActors.put(eventCounter, new DestinationAirportFields(((DestinationAirport_R)actor)._inTransit, ((DestinationAirport_R)actor)._transitExpires, ((DestinationAirport_R)actor)._called));
                }
                else if(actor instanceof  Track_R ){
                    stateSpace.get(stateIndex).areas.get(i).trackActors.put(eventCounter, new TrackFields(
                            ((Track_R)actor)._called, ((Track_R)actor)._inTransit, ((Track_R)actor)._OutRoute, ((Track_R)actor)._transitExpires));
                }
             eventCounter++;
            }
            
            i++;
        }
        
    }

    /**
     * This function is used to store the state after increasing the time.
     * In this function, only we need to store the event queue of the composite components
     * along with their actors.
     * @param currentStateIndex2
     * @throws IllegalActionException 
     */
    private void _storeStateTop(int stateIndex) throws IllegalActionException {
        // TODO Auto-generated method stub
        int i=-1;
        for(Object entity: entities) {
            i++;
            int eventCounter=0;
            
            stateSpace.get(stateIndex).areas.add(new AreaSnapshot());
            InsideDirector _director=((InsideDirector)((ControlArea)entity).getDirector());
            for(Object e: _director.getEventQueue().toArray()) {
//                if(((IntToken)((ControlArea)entity).areaId.getToken()).intValue()==currentUderAnalysisRegion)
                    stateSpace.get(stateIndex).areas.get(i).eventQueue.add((DEEvent)e);
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
                        stateSpace.get(stateIndex).areas.get(i).inputTokens.put(actor.getFullName()+port.getFullName(), temp);
                }
                // End of storing tokens on the ports
                if(actor instanceof Airport_R){
                    stateSpace.get(stateIndex).areas.get(i).airportActors.put(eventCounter,new AirportFeilds(
                            ((Airport_R)actor)._airplanes,((Airport_R)actor)._inTransit, ((Airport_R)actor)._transitExpires));
                }
                else if(actor instanceof DestinationAirport_R) {
                    stateSpace.get(stateIndex).areas.get(i).destinationAirportActors.put(eventCounter, new DestinationAirportFields(((DestinationAirport_R)actor)._inTransit, ((DestinationAirport_R)actor)._transitExpires, ((DestinationAirport_R)actor)._called));
                }
                else if(actor instanceof  Track_R ){
                    stateSpace.get(stateIndex).areas.get(i).trackActors.put(eventCounter, new TrackFields(
                            ((Track_R)actor)._called, ((Track_R)actor)._inTransit, ((Track_R)actor)._OutRoute, ((Track_R)actor)._transitExpires));
                }
             eventCounter++;
            }
        }
    }

    @Override
    public boolean postfire() throws IllegalActionException {

        
        //************Added by Maryam
        test++;
 //       if(test==96)
 //           System.out.println(test);
 //       System.out.println(stateSpace.size()-statesHasToRemoved.size());
       stateInAfterTime=-1;
        order.clear();
        dependentActors.clear();
        preToInternalB.clear();
        _internalTimedStates.clear();
        internalTimedStatesMap.clear();
        if(startTime>0 && (System.currentTimeMillis()-startTime)>=3600000) {
            timeOver=true;
            return false;
        }
        if(deadlockDetected==true)
        {
            return false;
        }

        try {
            _timeReachableStates.take();
            _timeReachableStatesMap.remove(_currentTimedState);
    
            if(!_timeReachableStates.isEmpty()){
                _noMoreActorsToFire=false;

                currentStateIndex=_timeReachableStates.get().stateIndex;
                if(currentStateIndex==225)
                    currentStateIndex=225;
                GlobalSnapshot temp =stateSpace.get(currentStateIndex);
                _currentTimedState=temp;
                
                _eventQueue.clear();
                Object[] eventArray=temp.eventQueue.toArray();
                for(Object object: eventArray){
                    _eventQueue.put((DEEvent)object);
                }
                _fillModelTop(temp);
            }
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //************
        
        boolean result = super.postfire();

        // If any output ports still have tokens to transfer,
        // request a refiring at the current time.
        CompositeActor container = (CompositeActor) getContainer();
        Iterator<IOPort> outports = container.outputPortList().iterator();
        boolean moreOutputsToTransfer = false;
        while (outports.hasNext() && !moreOutputsToTransfer) {
            IOPort outport = outports.next();
            for (int i = 0; i < outport.getWidthInside(); i++) {
                if (outport.hasNewTokenInside(i)) {
                    moreOutputsToTransfer = true;
                    break;
                }
            }
        }

        // Reset the microstep to zero if the next event is
        // in the future.
        synchronized (_eventQueueLock) {
            if (!_eventQueue.isEmpty() && !moreOutputsToTransfer) {
                DEEvent next = _eventQueue.get();
                if (next.timeStamp().compareTo(getModelTime()) > 0) {
                    _microstep = 0;
                }
            }
            boolean stop = ((BooleanToken) stopWhenQueueIsEmpty.getToken())
                    .booleanValue();

            // Request refiring and/or stop the model.
            // There are two conditions to stop the model.
            // 1. There are no more actors to be fired (i.e. event queue is
            // empty), and either of the following conditions is satisfied:
            //     a. the stopWhenQueueIsEmpty parameter is set to true.
            //     b. the current model time equals the model stop time.
            // 2. The event queue is not empty, but the current time exceeds
            // the stop time.
            if (moreOutputsToTransfer) {
                fireContainerAt(getModelTime());
            } else if (_noMoreActorsToFire && (stop
                    || getModelTime().compareTo(getModelStopTime()) == 0)) {
                if (_debugging) {
                    _debug("No more actors to fire and time to stop.");
                }
                _exceedStopTime = true;
                result = false;
            } else if (_exceedStopTime) {
                // If the current time is bigger than the stop time,
                // stop the model execution.
                result = false;
            } else if (isEmbedded() && !_eventQueue.isEmpty()) {
                // If the event queue is not empty and the container is an
                // embedded model, ask the upper level director in the
                // hierarchy to refire the container at the timestamp of the
                // first event of the local event queue.
                // This design allows the upper level director (actually all
                // levels in hierarchy) to keep a relatively short event queue.
                _requestFiring();
            }
        }
        if (isEmbedded()) {
            // Indicate that fireAt() requests should be passed up the
            // hierarchy if they are made before the next iteration.
            _delegateFireAt = true;
        }
        return result;
    }


    /**
     * This fillModel is used for the top-level director, when a set of components are fired
     * for the current time and will be fired for the next time step.
     * @param temp
     * @throws IllegalActionException 
     * @throws IllegalAccessException 
     */
    private void _fillModelTop(GlobalSnapshot temp) throws IllegalActionException, IllegalAccessException {
        // TODO Auto-generated method stub
        //Do not fill _eventQueue
        setModelTime(temp._modelTime);
        _microstep=temp._microstep;
        int i=0;
        for(Object entity: entities)
           ((InsideDirector)((ControlArea)entity).getDirector()).removedEvents.clear();
        
        for(AreaSnapshot area:temp.areas) {
            ((InsideDirector)((ControlArea)entities.get(i)).getDirector()).setEventQueue(area.eventQueue);
            int j=0;
            for(DEEvent object: area.eventQueue){
                NamedObj actor=(NamedObj)(object.actor());
                if(actor instanceof Track_R){
                    ((InsideDirector)((ControlArea)entities.get(i)).getDirector())._fillTrack(actor,area.trackActors.get(j));
                }
                if(actor instanceof Airport_R){
                    ((InsideDirector)((ControlArea)entities.get(i)).getDirector())._fillAirport(actor, area.airportActors.get(j));
                }
                if(actor instanceof DestinationAirport_R) {
//                    System.out.println("hi");
                    ((InsideDirector)((ControlArea)entities.get(i)).getDirector())._fillDestinationAirport(actor, area.destinationAirportActors.get(j));
                }
                j++;
            }
            i++;
        }  
    }
    
    /**
     * Check if a given event needs to be taken in parallel with the other events.
     * @param e
     * @return
     * @throws IllegalActionException 
     */
    public boolean inParallelEvent(DEEvent e) throws IllegalActionException {
        // TODO Auto-generated method stub
        
        
        if(e.actor() instanceof Track_R) {
            int currentEntity=entityNumber(((IntToken)((Track_R)e.actor()).trackId.getToken()).intValue(),0);
            Track_R track=(Track_R)e.actor();
            Track_R destTrack=null;
            int destEntity=entityNumber(track.destActor,0);
            if(destEntity==-1) { // destination is a destination airport
                destEntity=entityNumber(track.destActor,2);
                if(destEntity==-1)
                    throw new IllegalActionException("There is no destination airport with this name.");
            }
            else
                destTrack=((InsideDirector)(((ControlArea)entities.get(destEntity)).getDirector())).tracklists.get(track.destActor);

            if(track.isBoundary.getToken()==null || (destTrack!=null && destTrack.isBoundary.getToken()==null))
                return false; // if one of them is not boundary or pre-boundary
            if(((StringToken)track.isBoundary.getToken()).stringValue().equals("inb") && destTrack==null)
                return false; // an internal boundary actor sends to a destination airport
            if((((StringToken)track.isBoundary.getToken()).stringValue().equals("b") || 
                    ((StringToken)track.isBoundary.getToken()).stringValue().equals("pb"))
                    && destTrack==null)
                return false; // a boundary or pre-boundary actor sends to a destination airport
            if(((StringToken)track.isBoundary.getToken()).stringValue().equals("b") && destEntity!=currentEntity)
                return true;
            if(((StringToken)track.isBoundary.getToken()).stringValue().equals("b") && ((StringToken)destTrack.isBoundary.getToken()).stringValue().equals("b"))
                return true;
            if(((StringToken)track.isBoundary.getToken()).stringValue().equals("pb") && ((StringToken)destTrack.isBoundary.getToken()).stringValue().equals("b"))
                return true;
            if(((StringToken)track.isBoundary.getToken()).stringValue().equals("pb") && ((StringToken)destTrack.isBoundary.getToken()).stringValue().equals("pb"))
                return true;
            if(((StringToken)track.isBoundary.getToken()).stringValue().equals("b") && ((StringToken)destTrack.isBoundary.getToken()).stringValue().equals("pb"))
                return true;
        }
        else if(e.actor() instanceof Airport_R) {
            Airport_R airport=(Airport_R)e.actor();
            Track_R destTrack=null;
            int destEntity=entityNumber(airport.destActor,0);
            destTrack=((InsideDirector)(((ControlArea)entities.get(destEntity)).getDirector())).tracklists.get(airport.destActor);
            if(destTrack.isBoundary.getToken()==null)
                return false; // if one of them is not boundary or pre-boundary
            if( ((StringToken)destTrack.isBoundary.getToken()).stringValue().equals("b"))
                return true;
            if( ((StringToken)destTrack.isBoundary.getToken()).stringValue().equals("pb"))
                return true;
            
            return false;
        }
        else if(e.actor() instanceof DestinationAirport_R) {
            DestinationAirport_R dest=(DestinationAirport_R) e.actor();
            if(dest.isBoundary.getToken()==null || (dest.isBoundary.getToken()!=null && ((StringToken)dest.isBoundary.getToken()).stringValue().equals("inb")))
                return false; // if the destination airport is connected 
            // to an internal actor or an internal boundary actor
            else
                return true; // if it is connected to a boundary or pre-boundary actor.
        }
        
        return false;
    }

    
    /**
     * Given the id of an actor, this function recognizes the type of the chain 
     * ended to that actor.
     * @param name
     * @return
     * @throws IllegalActionException
     */
    //This function needs modification to contain pb to pb as in parallel
public String typeRecognize(DEEvent e) throws IllegalActionException {
        // TODO Auto-generated method stub
    if((e.actor() instanceof Airport_R) || (e.actor() instanceof DestinationAirport_R))
        return "T0";
    int name=((IntToken)(((Track_R)e.actor()).trackId.getToken())).intValue();
    int currentEntity=entityNumber(name,0);
    

    Track_R track=((Track_R)e.actor());
    Track_R destTrack=null;
    int destEntity=entityNumber(track.destActor,0);
    destTrack=((InsideDirector)((ControlArea)entities.get(destEntity)).getDirector()).tracklists.get(track.destActor);
    
    //check if the destination is outside
    boolean outside=false;
   if(destEntity!=-1 && destEntity!=currentEntity)
       outside=true;
    
    // if destination is outside and has token to send at current time
    boolean check=false;
    if(outside==true && destTrack._inTransit!=null && destTrack._transitExpires.equals(getModelTime()))
        check=true;
    
    boolean boundary=false;
    boolean internal=false;
    boolean pre_boundary=false;
    //check if the actor is internal
    
    if(track.isBoundary.getToken()==null)
        internal=true;
      //check if the actor is boundary
     
    else if(((StringToken)track.isBoundary.getToken()).stringValue().equals("b"))
        boundary=true;
    
    //check if the actor is pre-boundary
    
    else if(((StringToken)track.isBoundary.getToken()).stringValue().equals("pb"))
        pre_boundary=true;
    
 
//    if(track.isBoundary.getToken().toString().equals(""))
    
    String type="";
    
  //If it is boundary and depends to an outside actor
    if(boundary && check==true) {
        type="T1";
    }
    //If it is boundary, will send outside, but it is not dependent
    else if(boundary && outside && check==false) {
        type="T2";
    }
    // If it is boundary and will send to a boundary
    else if(boundary && outside==false && destTrack.isBoundary.getToken()!=null && destTrack.isBoundary.getToken().equals(new StringToken("b"))) {
        type="T2";
    }
    //If it is boundary, and will send to a pre-boundary
    else if(boundary && outside==false && destTrack.isBoundary.getToken()!=null && destTrack.isBoundary.getToken().equals(new StringToken("pb"))) {
        type="T0";
    }
    //If it is pre-boundary and will send to a boundary
    else if(pre_boundary && destTrack.isBoundary.getToken()!=null && destTrack.isBoundary.getToken().equals(new StringToken("b"))) {
        type="T2";
    }
    //If it is internal, or it is a pre-boundary which will send to an internal,
    //*** Please note that after solving some events, these type may change to type T2.
    else if(internal || (pre_boundary && destTrack.isBoundary.getToken()==null)) {
        type="T0";
    }
    return type;
    
   }

/**
 * Return the index of the composite actor containing this actor in entities list,
 * The  parameter type means we mean a track or a source airport or a destination airport
 * @param name
 * @return
 */
private int entityNumber(Integer name,int type) {
        // TODO Auto-generated method stub
    
    
    for(int i=0;i<numberOfRegions;i++) {
        switch(type) {
        case 0:
        {
            if(((InsideDirector)((ControlArea)entities.get(i)).getDirector()).tracklists.containsKey(name))
                return i;
            else
                break;
        }
        case 1:
        {
            if(((InsideDirector)((ControlArea)entities.get(i)).getDirector()).sourceAirports.containsKey(name))
                return i;
            else
                break;
        }
        case 2:
        {
            if(((InsideDirector)((ControlArea)entities.get(i)).getDirector()).destinationAirports.containsKey(name))
                return i;
            else
                break;
        }
        
        }
    } 
    return -1;
    }


    /**
     * Return the id of the region containing an actor
     * @param name
     * @return
     * @throws IllegalActionException 
     */
    int regionNumber(int name, int type) throws IllegalActionException {
        for(int i=0;i<numberOfRegions;i++) {
            switch(type) {
            case 0:
            {
                if(((InsideDirector)((ControlArea)entities.get(i)).getDirector()).tracklists.containsKey(name))
                    return ((IntToken)((ControlArea)entities.get(i)).areaId.getToken()).intValue(); 
            }
            case 1:
            {
                if(((InsideDirector)((ControlArea)entities.get(i)).getDirector()).sourceAirports.containsKey(name))
                    return ((IntToken)((ControlArea)entities.get(i)).areaId.getToken()).intValue();
            }
            case 2:
            {
                if(((InsideDirector)((ControlArea)entities.get(i)).getDirector()).destinationAirports.containsKey(name))
                    return ((IntToken)((ControlArea)entities.get(i)).areaId.getToken()).intValue();
            }
            
            }
        }
        return -1;
    }

    
 // Assume that a route is {1,(2,1.5),(3,2.5);2,(4,3.5),(5,4.5)}. 
    // In this route, 1 and 2 show the region's id, and each tuple shows a track and arrival time 
    // at the track. 
    private void _readFromFile() throws IOException, IllegalActionException {
        
        int iteration=0;
        String fileName="";
        if(((IntToken)indexOfFile.getToken()).intValue()==0) {
            fileName="input.txt";
        }
        else {
            fileName="input"+((IntToken)indexOfFile.getToken()).intValue()+".txt"; 
        }
        
        BufferedReader bufferedReader =  new BufferedReader(new

                FileReader(fileName));

        int counter=0;
        String line="";
        while((line=bufferedReader.readLine())!=null) {
            line=line.substring(1,line.length()-1);
            String [] parts=line.split(";");
            int targetRegionId=Integer.valueOf(parts[0].substring(0,parts[0].split(",")[0].length()));
            int i=0;
            for(i=0;i<entities.size();i++)
                if(((IntToken)((ControlArea)entities.get(i)).areaId.getToken()).intValue()==targetRegionId)
                    break;
            String[] travelingMap=parts[0].substring(parts[0].split(",")[0].length()+1).split(",");
            String temp=travelingMap[0];
            temp=temp.substring(1, temp.length()-1);
            String[] temp2=temp.split("/");

            Track_R track=((InsideDirector)((ControlArea)entities.get(i)).getDirector()).tracklists.get(Integer.valueOf(temp2[0]));
            Airport_R airport=((InsideDirector)((ControlArea)entities.get(i)).getDirector()).sourceAirports.get(((IntToken)track.connectedSourceA.getToken()).intValue());
            
            String map="";
            
            for(int j=0;j<parts.length;j++) {
                temp="";
                if(j==parts.length-1)
                    temp=parts[j].substring(parts[j].split(",")[0].length()+1,parts[j].length());
                else
                    temp=parts[j].substring(parts[j].split(",")[0].length()+1);
                map+=temp;
                if(j!=parts.length-1)
                    map+=";";
            }
            
            Map<String, Token> aircraft = new TreeMap<String, Token>();
            aircraft.put("aircraftId", new IntToken(counter));
            aircraft.put("aircraftSpeed", new IntToken(200));
            aircraft.put("flightMap", new StringToken(map));
            aircraft.put("priorTrack", new IntToken(-1));
            aircraft.put("fuel", new DoubleToken(350));
            ((InsideDirector)((ControlArea)entities.get(i)).getDirector())._LoadAirport(airport,Double.valueOf(temp2[1]),new RecordToken(aircraft));
            counter++;
            }
        
        sendRequestTop();
    }
/**
 * This function is used to create an event for the top-level coordinator. It is used when a file 
 * is read and the events of all composite actors are filled. Then it create an event per each control area with a non-empty internal eventqueue.
 */
    private void sendRequestTop() {
    // TODO Auto-generated method stub
    for(int i=0;i<entities.size();i++)
        try {
            if(!((InsideDirector)((ControlArea)entities.get(i)).getDirector()).getEventQueue().isEmpty()) {
                DEEvent e=((InsideDirector)((ControlArea)entities.get(i)).getDirector()).getEventQueue().get();
                Actor a=(Actor) ((ControlArea)entities.get(i));
                fireAt(a, e.timeStamp());
            }
        } catch (InvalidStateException | IllegalActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
}
    
    /**
     * This function return the actor who is destination of the given token. This function is used
     * in the ATCReceiver_R
     * @param token
     * @return
     */
    public Track_R returnDesTrack(Token token) {
        String map = ((StringToken) ((RecordToken)token).get("flightMap")).stringValue();
        int trackId=Integer.valueOf(map.split(";")[0].split(",")[0].split("/")[0].substring(1));
        int index=entityNumber(trackId, 0);
        return ((InsideDirector)((ControlArea)entities.get(index)).getDirector()).tracklists.get(trackId);
        // TODO Auto-generated method stub
        
    }
    
    public void storeDependentActor(int sender, int receiver) {
        dependentActors.put(receiver, new info(sender));
    }
    
    public void updateReqInfoOfDependentActors(int receiver, boolean isReq) {
        if(dependentActors.containsKey(receiver))
        {
            info temp=new info(dependentActors.get(receiver).trackId);
            temp.beenOcc=dependentActors.get(receiver).beenOcc;
            temp.beenReq=isReq;
            
            dependentActors.remove(receiver);
            dependentActors.put(receiver, temp);
        }
    }
    
    public void updateOccInfoOfDependentActors(int receiver, boolean isOcc) {
        if(dependentActors.containsKey(receiver))
        {
            info temp=new info(dependentActors.get(receiver).trackId);
            temp.beenOcc=dependentActors.get(receiver).beenReq;
            temp.beenReq=isOcc;
            
            dependentActors.remove(receiver);
            dependentActors.put(receiver, temp);
        }
    }
    
    
    /**
     * Return source of a message
     * @param token
     * @return
     */
    public Track_R returnSourceTrack(Token token) {
        // TODO Auto-generated method stub
        RecordToken temp=(RecordToken) token;
        int priorTrack=((IntToken)temp.get("priorTrack")).intValue();
        int entityNum=entityNumber(priorTrack, 0);
        if(entityNum==-1)
            return null;
        return ((InsideDirector)((ControlArea)entities.get(entityNum)).getDirector()).tracklists.get(priorTrack);
    }
    
    public void addTpreToInternalB(int intValue, boolean b) {
        preToInternalB.put(intValue, b);
    }
    
    public boolean containPreb(int id) {
        // TODO Auto-generated method stub
        if(preToInternalB.containsKey(id))
            return true;
        return false;
    }

    /**
     * The number of control areas
     */
    int numberOfRegions;
    
    /**
     * The list of control areas
     */
    List entities;
    /**
     * True if a deadlock (livelock) happens in the model. The deadlock happens when at least a moving object runs out of fuel.  
     */
    public boolean deadlockDetected;
    
    /**
     * If the execution time goes more than one houre. 
     */
    private boolean timeOver;
    
    private long startTime; 
    
    public int currentStateIndex;
    
    public TimedStatesList _timeReachableStates;
    public HashMap<GlobalSnapshot, Integer> _timeReachableStatesMap;
    
    public ArrayList<GlobalSnapshot> stateSpace;
    
    /**
     * The set of timed states which are generated by firing the components are
     * stored. The final states which are generated per firing of each composite actor.
     */
    public ArrayList<GlobalSnapshot> _internalTimedStates;
    
    /**
     * To know the index of the interalTimedStates in the statespace.
     */
    public HashMap<GlobalSnapshot, Integer> internalTimedStatesMap;
    
    /**
     * This variable is set in _fire and is used to set currentCArea in global state. 
     */
    public int currentUderAnalysisRegion;
    
    /**
     * Current global timed state that its underlying state space is generated.
     */
    private GlobalSnapshot _currentTimedState;
    
    /**
     * This event queue is used to store the events which should be taken in parallel.
     */
    public DEEventQueue _internalEventQueue;
    
    /**
     *  To store states generated between the final internal time state and the real time state.
     */
    private HashMap<GlobalSnapshot, Integer> tempStates;
    
    
    /**
     * If a state is visited twice
     */
    private int _recentlyStVisited;
    
    private boolean _internalStopFireRequested;
    
    private int _nextStateIndex;
    
    public ArrayList<GlobalSnapshot> _tempInternalTimedStates;
    public HashMap<GlobalSnapshot, Integer> _tempInternalTimedStatesMap;
    
    /**The dimension of the network which is n*n. */
    public int dimension;
    
    /**The dimension of a region: number of the regions are (n/regionDimension)*(n/regionDimension). */
    public int regionDimension;
    
    private boolean fileRead;

    /**
     * This array stores which internal boundary actor depends to which pre-boundary actor.
     * The key is pre-boundary id, and the value stores the internal boundary actor along
     * with some information about the pre-boundary actor.
     */
    public HashMap<Integer, info> dependentActors;
    
    
    /**
     * This array stores in the sequential phase if a pre-boundary actor
     * sends a message to an internal boundary actor. This array is useful in the
     * parallel execution where an actor sends a message to a pre-boundary actor and the acceptance
     * or rejection of the send is determined based on the acceptance or rejection of pre-boundary to internal boundary.
     * The value is false.
     */
    public HashMap<Integer,Boolean> preToInternalB;
    
    
    /**
     * in which order in a time step, we fire the composite actors.
     */
    public ArrayList<Integer> order;

    /**
     * In parallel execution of actors, we do not have any triggering event unless a send from internal
     * boundaries has been accepted. In this situation this variable for the current selected actor is set to true;
     */
    private boolean hasBeenAccepted;
    
    /**
     * If we are in the parallel execution phase of the actors, this variable is true. Otherwise, it is false;
     */
    public boolean inParallelExecution;
    
    /**
     * If the message of a pre-boundary actor to a boundary actor is rejected, this variable sets true;
     */
    public boolean rejected;
    
    /**
     * This array stores the index of the states of the state space which should be removed.
     * When we diagnose that a final state of the parallel phase is invalid, we remove it up to the parent of that part.
     * We should go up into the tree of the sequential part, until we reach to the reject, or accept part of the actor who causes the problem.
     */
    private HashMap<Integer,Boolean> statesHasToRemoved;
    
    public Track_R boundaryTrack;
    
    /**
     * This variable keeps the index of the state in which the time is greater than the time of its parent.
     */
    private int stateInAfterTime;
    
    /**
     * If in the parallel exection, the message of an actor to a pre-boundary actor has to be rejected.
     */
    public boolean isRejectPhase;
    
    public int test;

    /**
     * Count the number of all states
     */
    public int _numberOfAllStates;
    
    /**
     * Count the number of timed states
     */
    
    public int _numberOfTimedStates;
    
    

    
    
   

}
