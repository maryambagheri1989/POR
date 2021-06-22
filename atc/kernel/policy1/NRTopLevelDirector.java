/**
 * This is a flat model to compare its performance with the TopLevelDirector.
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
import java.util.Map.Entry;
import java.util.TreeMap;

import ptolemy.actor.Actor;
import ptolemy.actor.CompositeActor;
import ptolemy.actor.Director;
import ptolemy.actor.FiringEvent;
import ptolemy.actor.IOPort;
import ptolemy.actor.Manager;
import ptolemy.actor.Receiver;
import ptolemy.actor.SuperdenseTimeDirector;
import ptolemy.actor.util.Time;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.domains.atc.kernel.ATCReceiver;
import ptolemy.domains.atc.kernel.AirportFeilds;
import ptolemy.domains.atc.kernel.DestinationAirportFields;
import ptolemy.domains.atc.kernel.NRRegion;
import ptolemy.domains.atc.kernel.Snapshot;
import ptolemy.domains.atc.kernel.TrackFields;
import ptolemy.domains.atc.lib.Airport;
import ptolemy.domains.atc.lib.Track_NR;
import ptolemy.domains.atc.lib.DestinationAirport;
import ptolemy.domains.atc.lib.TimedStatesList;
import ptolemy.domains.de.kernel.DEEvent;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Nameable;
import ptolemy.kernel.util.NamedObj;

public class NRTopLevelDirector extends ATCDirector{

    public NRTopLevelDirector(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        // TODO Auto-generated constructor stub
        numberOfRegions = new Parameter(this, "numberOfRegions");
        numberOfRegions.setExpression("1");
        numberOfRegions.setTypeEquals(BaseType.INT);
        
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
    
    public Parameter numberOfRegions;
    public Parameter networkDimension;
    public Parameter dimentionOfRegion;
    public Parameter indexOfFile;
    
    
    @Override
    public void initialize() throws IllegalActionException {
        _stateSpace=new ArrayList<Snapshot>();
        _stopAnalysis=false;
        _informationLoaded=false;
        _timeReachableStates=new TimedStatesList(null, null);
        _timeReachableStatesMap=new HashMap<>();
        arrivalTimeToNet=new HashMap<>();
        _currentTimedState=null;
        startTime=0;
        deadlockDetected=false;
        timeOver=false;
        _currentStateIndex=-1;
        _numberOfAllStates=0;
        _numberOfTimedStates=0;
        test=0;
        airplanes=new TreeMap<>();
        regions=new ArrayList<NRRegion>();
        numOfRegions=((IntToken)numberOfRegions.getToken()).intValue();
        dimension=((IntToken)networkDimension.getToken()).intValue();
        regionDimension=((IntToken)dimentionOfRegion.getToken()).intValue();
        for(int i=1;i<=numOfRegions;i++)
        {
            regions.add(new NRRegion(i));
        }
        super.initialize();
        
    }
    
    @Override
    public void handleInitializedTrack(NamedObj track) throws IllegalActionException {
        int region=((IntToken) ((Track_NR)track).regionId).intValue();
        int id=((IntToken) ((Track_NR)track).trackId.getToken()).intValue();
        regions.get(region-1).tracks.put(id, (Track_NR)track);
        ArrayToken borders=(ArrayToken) ((Track_NR)track).border.getToken();
        if(borders!=null)
        for(int i=0;i<borders.length();i++) {
            String temp=((StringToken) borders.getElement(i)).stringValue();
            if(temp!="")
            {

                int regionId=Integer.valueOf(temp.substring(1));
                switch(temp.substring(0,1)) {
                case "N":
                {

                    regions.get(regionId-1).northNeighbors.put(id,(Track_NR)track);
                    regions.get(regionId-1).neighbors.put(0, regions.get(region-1));
                    break;
                }
                case "W":
                {

                    regions.get(regionId-1).westNeighbors.put(id,(Track_NR)track);
                    regions.get(regionId-1).neighbors.put(3, regions.get(region-1));
                    break;
                }
                case "S":
                {
                    regions.get(regionId-1).southNeighbors.put(id,(Track_NR)track);
                    regions.get(regionId-1).neighbors.put(2, regions.get(region-1));
                    break;
                }
                case "E":
                {
                    regions.get(regionId-1).eastNeighbors.put(id,(Track_NR)track);
                    regions.get(regionId-1).neighbors.put(1, regions.get(region-1));
                }
                }
            }
        }
        super.handleInitializedTrack((Track_NR)track);
    }
    
    @Override
    public void wrapup() throws IllegalActionException {
        
        int iteration=0;
        String outputFileStates="";
        String outputFileTime="";
        String outputFileAllStates="";
        String outputTimedStates="";
        
        Director executiveDirector = ((CompositeActor) getContainer())
                .getExecutiveDirector();
      
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
        _informationLoaded=false;
        _stopAnalysis=false;
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
                if(startTime==0) {
                    _writerTime.print(0+"\n");
                }
                else {
                  _writerTime.print((System.currentTimeMillis()-startTime)+"\n");
                }
                _writer.print(_stateSpace.size()+"\n");
                _writerAllStates.print(_numberOfAllStates+"\n");
                _writerTimedStates.print(_numberOfTimedStates+"\n");
              System.out.println("The number of states: "+_stateSpace.size()+"\n");
              System.out.println("The number of all States: "+_numberOfAllStates+"\n");
              System.out.println("The number of timed States: "+_numberOfTimedStates+"\n");
            }
            _writerTime.close();
            

            _writer.close();
            _writerAllStates.close();
            _writerTimedStates.close();
            deadlockDetected=false;
            timeOver=false;
            _stateSpace.clear();
            _timeReachableStates.clear();
            _timeReachableStatesMap.clear();
            _numberOfAllStates=0;
            _numberOfTimedStates=0;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    
    /** Reroute an aircraft.
     *  @param aircraft The aircraft
     *  @return A Map of rerouted aircraft.
     *  @exception IllegalActionException If thrown while getting the flightMap or setting parameters.
     */
    @Override
    public Map<String, Token> rerouteUnacceptedAircraft(Token aircraft,int regionId)
            throws IllegalActionException {
        
        RecordToken airplane = (RecordToken) aircraft;
        String flightMap = ((StringToken) airplane.get("flightMap")).stringValue();
        String [] parts=flightMap.split(";")[0].split(",");
        double currentTime=Double.valueOf(parts[0].split("/")[1].substring(0,parts[0].split("/")[1].length()-1));
        flightMap=rebuildRouteWithNewTime(currentTime, flightMap);
        Map<String, Token> map = new TreeMap<String, Token>();
        map.put("flightMap", new StringToken(flightMap));
        map.put("route", new IntToken(-1));
        map.put("delay", new DoubleToken(1.0));
        return map;
    }
    
    
    /**
     * It receive a path and time and rebuild the path starting with the given time
     * @param lastTime
     * @param tempMap
     * @return
     */
    
    private String rebuildRouteWithNewTime(double lastTime, String tempMap) {
        // TODO Auto-generated method stub
        String[] parts=tempMap.replaceAll(";", ",").split(",");
        String newMap="";
        int count=0;
        for(int i=0;i<parts.length;i++)
        {
            if(!parts[i].equals("")) {
                int trackId=Integer.valueOf(parts[i].split("/")[0].substring(1));
                lastTime+=1;
                newMap+="("+trackId+"/"+lastTime+")";
                count+=parts[i].length();
                if(tempMap.length()-1>count)
                    newMap+=tempMap.charAt(count);
            }
            else {
                newMap+=tempMap.charAt(count);
            }
            count+=1;
        }
        return newMap;
    }
    
    @Override
    public boolean postfire() throws IllegalActionException {
        
        //************Added by Maryam
        if(startTime>0 && System.currentTimeMillis()-startTime>=3600000) {
            timeOver=true;
            return false;
        }
        if(_stopAnalysis==true)
            return false;
        
        if(deadlockDetected==true)
        {
            return false;
        }

        try {
            _timeReachableStates.take();
            _timeReachableStatesMap.remove(_currentTimedState);
            

            if(!_timeReachableStates.isEmpty()){
                _noMoreActorsToFire=false;
                _currentStateIndex=_timeReachableStates.get().stateIndex;
                Snapshot temp =_stateSpace.get(_currentStateIndex);
                _currentTimedState=temp;
                

                _eventQueue.clear();
                Object[] eventArray=temp.eventQueue.toArray();
                for(Object object: eventArray){
                    _eventQueue.put((DEEvent)object);
                }
                _fillModel(temp);
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
        // NOTE: The following commented block enforces that no events with
        // different tags can exist in the same receiver.
        // This is a quite different semantics from the previous designs,
        // and its effects are still under investigation and debate.
        //        // Clear all of the contained actor's input ports.
        //        for (Iterator actors = ((CompositeActor)getContainer())
        //                .entityList(Actor.class).iterator();
        //                actors.hasNext();) {
        //            Entity actor = (Entity)actors.next();
        //            Iterator ports = actor.portList().iterator();
        //            while (ports.hasNext()) {
        //                IOPort port = (IOPort)ports.next();
        //                if (port.isInput()) {
        //                    // Clear all receivers.
        //                    Receiver[][] receivers = port.getReceivers();
        //                    if (receivers == null) {
        //                        throw new InternalErrorException(this, null,
        //                                "port.getReceivers() returned null! "
        //                                + "This should never happen. "
        //                                + "port was '" + port + "'");
        //                    }
        //                    for (int i = 0; i < receivers.length; i++) {
        //                        Receiver[] receivers2 = receivers[i];
        //                        for (int j = 0; j < receivers2.length; j++) {
        //                            receivers2[j].clear();
        //                        }
        //                    }
        //                }
        //            }
        //        }
        return result;
    }

    
    @Override
    protected Actor _getNextActorToFire() throws IllegalActionException {
        if (_eventQueue == null) {
            throw new IllegalActionException(
                    "Fire method called before the preinitialize method.");
        }
        //**********Added by Maryam
        boolean timeIncreased=false;
      //**********
        
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
                        _stateSpace.get(_nextStateIndex).upToThisTaken++;
                    }
                    else
                    {
                        _stateSpace.get(_currentStateIndex).upToThisTaken++;
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
                      //*************Added by Maryam
                        // Although this would not happen for us, because in our model
                        // every track actor is fired through only one event
                        if(timeIncreased==true)
                        {
                            // We have a timed transition
                            _stateSpace.get(_nextStateIndex).upToThisTaken++;
                        }
                        else
                        {
                            _stateSpace.get(_currentStateIndex).upToThisTaken++;
                        }
                        //*************
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
    
    
    protected boolean _checkForNextEventInParent() throws IllegalActionException {
        // The following code enforces that a firing of a
        // DE director only handles events with the same tag.
        // If the earliest event in the event queue is in the future,
        // this code terminates the current iteration.
        // This code is applied on both embedded and top-level directors.
//        Object[] objectArray=_stateSpace.get(_currentStateIndex).eventQueue.toArray();
        if(_stateSpace.get(_currentStateIndex).eventQueue.size()!=0 && 
                _stateSpace.get(_currentStateIndex).upToThisTaken+1 < _stateSpace.get(_currentStateIndex).eventQueue.size()) {
            DEEvent next=_stateSpace.get(_currentStateIndex).eventQueue.get(_stateSpace.get(_currentStateIndex).upToThisTaken+1);
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
    
    
    int roundCounter = 0;
    @Override
    public void fire() throws IllegalActionException {
        if (_debugging) {
            _debug("========= " + this.getName() + " director fires at "
                    + getModelTime() + "  with microstep as " + _microstep);
        }
        
        if(_informationLoaded==false) {
            _informationLoaded=true;
            try {
                _readFromFile();
                startTime=System.currentTimeMillis();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        roundCounter++;
        
        //**********Added by Maryam
        ArrayList<Integer[]> forkNodes=new ArrayList<Integer[]>(); 
        tempStates=new HashMap<>();
        //******************
        // NOTE: This fire method does not call super.fire()
        // because this method is very different from that of the super class.
        // A BIG while loop that handles all events with the same tag.
        int counter = 0;
        while (true) {  
            int result = _fire();
            
           //*********Added by Maryam
            if(deadlockDetected==true) {
                _stopAnalysis=true;
                return;
            }
           //****end
            
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
            //************ Changed by Maryam
            if ( recentlyTimeStVisited!=-1 || !_checkForNextEvent()) {
                //************ Added by Maryam
                boolean flag=false;
                int parentIndex=-1;
                if(recentlyTimeStVisited==-1) {
                    // If this variable is not -1, then we are visiting a duplicative state
                    // in tempState and have to remove it from _stateSpace... Otherwise the state
                    // is a timed state

                        int x=-1;
                        if(_timeReachableStatesMap.containsKey(_stateSpace.get(_currentStateIndex))) {
                            x=_timeReachableStatesMap.get(_stateSpace.get(_currentStateIndex)); //returns index in stateSpace array
                        }

                        if(x!=-1) { // timeReachableStates contains temp2
                            parentIndex=_stateSpace.get(_currentStateIndex).parent.get(0);
                            _cleanModel(_stateSpace.get(_currentStateIndex).eventQueue);
                            _stateSpace.remove(_currentStateIndex);
                           _stateSpace.get(x).parent.add(parentIndex);
                        }
                        else {
                            _timeReachableStates.add(_currentStateIndex, _stateSpace.get(_currentStateIndex)._modelTime);
                            _timeReachableStatesMap.put(_stateSpace.get(_currentStateIndex), _currentStateIndex);
                            
                            _numberOfTimedStates++;
                        }

                }
                
                do{
                    if(parentIndex==-1) {
                        _cleanModel(_stateSpace.get(_currentStateIndex).eventQueue);
                        _currentStateIndex=_stateSpace.get(_currentStateIndex).parent.get(0);
                    }
                    else {
                        _currentStateIndex=parentIndex;
                        parentIndex=-1;
                    }
                    if(!forkNodes.isEmpty() && _currentStateIndex<forkNodes.get(forkNodes.size()-1)[0])
                    {
                        // We have passed the node in forkCounter and reached its parent
                        if(recentlyTimeStVisited!=-1) {
                            forkNodes.get(forkNodes.size()-1)[1]--;
                            int t=recentlyTimeStVisited;
                            while(forkNodes.get(forkNodes.size()-1)[0]!=t)
                            {
                                int parent=_stateSpace.get(t).parent.get(0);
                                _stateSpace.remove(t);
                                t=parent;
                                
                            }
                            recentlyTimeStVisited=-1;
                        }
                        if(forkNodes.get(forkNodes.size()-1)[1]==0)
                        {
                            //No child remains for this node and this node has to be removed.
                            recentlyTimeStVisited=forkNodes.get(forkNodes.size()-1)[0];
                            forkNodes.remove(forkNodes.size()-1);

                        }
                    }
                    if(_stateSpace.get(_currentStateIndex).upToThisTaken==-1)
                    {
                        flag=true;

                        forkNodes.clear();
                        tempStates.clear();
                        break;
                    }

                } while(!_checkForNextEventInParent());
                 if(flag){
                    break;
                }

                
                if(!forkNodes.isEmpty() && forkNodes.get(forkNodes.size()-1)[0]==_currentStateIndex) //We've already added this fork  node
                {
                    if(recentlyTimeStVisited==-1){
                        Integer [] a={forkNodes.get(forkNodes.size()-1)[0],forkNodes.get(forkNodes.size()-1)[1]+1};
                        forkNodes.remove(forkNodes.size()-1);
                        forkNodes.add(a);
                    }
                    else{
                        int t=recentlyTimeStVisited;
                        while(_currentStateIndex!=t)
                        {
                            int parent=_stateSpace.get(t).parent.get(0);
                            _stateSpace.remove(t);
                            t=parent;
                            
                        }
                        recentlyTimeStVisited=-1;
                    }
                    
                    
                }
                else{
                    if(recentlyTimeStVisited==-1){
                        Integer [] a={_currentStateIndex,2};
                        forkNodes.add(a);
                    }
                    else{
                        Integer [] a={_currentStateIndex,1};
                        forkNodes.add(a);
                        int t=recentlyTimeStVisited;
                        while(_currentStateIndex!=t)
                        {
                            int parent=_stateSpace.get(t).parent.get(0);
                            _stateSpace.remove(t);
                            t=parent;
                            
                        }
                        recentlyTimeStVisited=-1;
                        
                    }
                }
                //Fill _eventQueue
                _eventQueue.clear();
                for(int i=_stateSpace.get(_currentStateIndex).upToThisTaken+1;i<_stateSpace.get(_currentStateIndex).eventQueue.size();i++)
                    _eventQueue.put(_stateSpace.get(_currentStateIndex).eventQueue.get(i));
                // Fill MODEL
                _fillModel(_stateSpace.get(_currentStateIndex));
                    
                //*************
                
             //************Removed by Maryam   
              //  break;
              //*************
            } // else keep executing in the current iteration
            counter ++;
//            if (counter % 1000 == 0)
//                System.out.println(counter);
        } // Close the BIG while loop.
//        System.out.println("Total:" + counter);
        // Since we are now actually stopping the firing, we can set this false.
        _stopFireRequested = false;

        if (_debugging) {
            _debug("DE director fired!");
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
            if(actor instanceof Track_NR){
                ((Track_NR)actor).cleanTrack();
                _inTransit.put(((IntToken)(((Track_NR)actor).trackId.getToken())).intValue(), false);
            }
            if(actor instanceof Airport){
                ((Airport)actor)._airplanes.clear();
                ((Airport)actor)._inTransit=null;
            }
            if(actor instanceof DestinationAirport){
                ((DestinationAirport)actor).cleanDestinationAirport();
            }
        }
//        underAnalysisMO.clear();
    }

    private void _fillModel(Snapshot state) throws IllegalActionException {
        // Don't change _eventQueue, only fill actors and ports, and modelTime
      
        for(Entry<Integer, Boolean> entry:state.regionsUnderAnalysis.entrySet())
        {
            regions.get(entry.getKey()-1).underAnalysis=entry.getValue();
            regions.get(entry.getKey()-1).aircraftHasArrived.clear();
        }
        
        for(int j=0;j<state.isAircraftArrived.size();j++) {
            String[] temp=state.isAircraftArrived.get(j).split(",");
            regions.get(Integer.valueOf(temp[0])-1).aircraftHasArrived.add(Integer.valueOf(temp[1]));
        }
        
        setModelTime(state._modelTime);
        _microstep=state._microstep;
        
//        underAnalysisMO=new HashMap<>();
//        for(Entry<Integer,String> entry: state.underAnalysisMovingObjects.entrySet())
//            underAnalysisMO.put(entry.getKey(), entry.getValue());
        
        int i=0;
        for(DEEvent object: state.eventQueue){
            NamedObj actor=(NamedObj)(object.actor());
            if(actor instanceof Track_NR){
                _fillTrack(actor,state.trackActors.get(i));
            }
            if(actor instanceof Airport){
                _fillAirport(actor, state.airportActors.get(i));
            }
            if(actor instanceof DestinationAirport) {
                _fillDestinationAirport(actor, state.destinationAirportActors.get(i));
            }
            
            IOPort port=object.ioPort();
            if(port!=null){
                String name=object.actor().getFullName()+port.getFullName();
                _fillInputPort(object,state.inputTokens.get(name));
            }  
            i++;
        }
    }
    
    private void _fillTrack(NamedObj actor, TrackFields trackFields) throws IllegalActionException {
        // TODO Auto-generated method stub
        ((Track_NR) actor).generator=trackFields.genMode;
        ((Track_NR) actor)._called=trackFields.called;
        ((Track_NR) actor)._inTransit=trackFields.inTransit;
        ((Track_NR) actor)._OutRoute=trackFields.OutRoute;
        ((Track_NR) actor)._transitExpires=trackFields.transitExpires;
        
        
        if(trackFields.genMode==false)
            if(trackFields.inTransit!=null )
                _inTransit.put(((IntToken)((Track_NR) actor).trackId.getToken()).intValue(), true);
            else{
                _inTransit.put(((IntToken)((Track_NR) actor).trackId.getToken()).intValue(), false);
            }
        
    }

    @Override
    protected int _fire() throws IllegalActionException {
        // Find the next actor to be fired.
        //********************Added by Maryam
      
        if(_currentStateIndex==-1)
        {
            // Create initial state
            _currentStateIndex=0;
            _stateSpace.add(new Snapshot(-1, getEventQueue(), null, getModelTime(),_microstep, null));
            
            _numberOfAllStates++;
            
            _storeState(_currentStateIndex);

            _timeReachableStates.add(_currentStateIndex, getModelTime());
            _currentTimedState= _stateSpace.get(_currentStateIndex);
            _timeReachableStatesMap.put(_currentTimedState, _currentStateIndex);
            
            _numberOfTimedStates++;
          
        }
        // If we reach this point and _eventQueue is not null but the eventQueue in currentStateIndex
        // is null, means that the _eventQueue has changed in between and we have to update the eventQueue of currentStateIndex,
        // we also need to storeState(_currentStateIndex)
        if(_stateSpace.get(_currentStateIndex).eventQueue.isEmpty() && !_eventQueue.isEmpty())
        {
            Object[] objectArray=getEventQueue().toArray();
            for(Object object: objectArray)
                _stateSpace.get(_currentStateIndex).eventQueue.add((DEEvent) object);
//                _stateSpace.get(_currentStateIndex).eventQueue=getEventQueue();
                _storeState(_currentStateIndex);
        }
        
        // If we have a time transition, this state will be the target state, otherwise,
        // this will be the state which is obtained by firing an actor.
        // The information of this state will be updated. 
        _stateSpace.add(new Snapshot(_currentStateIndex,null,null,getModelTime(),_microstep,null));
        _nextStateIndex=_stateSpace.size()-1; // For time _nextStateIndex for immediate, current
        //***********************
        
        Actor actorToFire = _getNextActorToFire();
        
        //********************Added by Maryam
        if(actorToFire==null){
            _stateSpace.remove(_nextStateIndex);
        }
        
        _numberOfAllStates++;
        //***********************
        
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
        
        //********************Added by Maryam
        if(_stateSpace.get(_currentStateIndex)._modelTime.compareTo(getModelTime())<0){
            // Model time has been increased, and we have a timed transition.
            _stateSpace.get(_nextStateIndex)._modelTime=getModelTime();
            _stateSpace.get(_nextStateIndex)._microstep=_microstep;
            _copyState(_nextStateIndex,_stateSpace.get(_currentStateIndex));
            _currentStateIndex=_nextStateIndex;
            _stateSpace.add(new Snapshot(_currentStateIndex,null,null,getModelTime(),_microstep, null));
            _nextStateIndex=_stateSpace.size()-1;
            
            _numberOfAllStates++;
        }
        
        _stateSpace.get(_nextStateIndex).name=actorToFire.getFullName();
        _stateSpace.get(_nextStateIndex)._microstep=_microstep;

        //***********************
        
        
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
        
        //***************Added by Maryam


        int upToThis=_stateSpace.get(_currentStateIndex).upToThisTaken;
        
        for(int i=0;i<upToThis;i++)
            _eventQueue.put(_stateSpace.get(_currentStateIndex).eventQueue.get(i));
        
        Object[] objectArray=_eventQueue.toArray();
        for(Object object: objectArray)
            _stateSpace.get(_nextStateIndex).eventQueue.add((DEEvent) object);
        
        
        _storeState(_nextStateIndex);
        _currentStateIndex=_nextStateIndex;
        
        // Check whether the _currentStateIndex exists in tempStates

        if(tempStates.containsKey(_stateSpace.get(_currentStateIndex)))
            recentlyTimeStVisited=_currentStateIndex;
        else
            tempStates.put(_stateSpace.get(_currentStateIndex), _currentStateIndex);
        //***********************
        return 0;
    }
    
    
    private void _copyState(int _nextStateIndex, Snapshot snapshot) {
        // TODO Auto-generated method stub
        _stateSpace.get(_nextStateIndex).eventQueue=snapshot.eventQueue;
        _stateSpace.get(_nextStateIndex).airportActors=snapshot.airportActors;
        _stateSpace.get(_nextStateIndex).destinationAirportActors=snapshot.destinationAirportActors;
        _stateSpace.get(_nextStateIndex).inputTokens=snapshot.inputTokens;
        _stateSpace.get(_nextStateIndex).trackActors=snapshot.trackActors;
        _stateSpace.get(_nextStateIndex).regionsUnderAnalysis=snapshot.regionsUnderAnalysis;
        _stateSpace.get(_nextStateIndex).isAircraftArrived=snapshot.isAircraftArrived;
        _stateSpace.get(_nextStateIndex).underAnalysisMovingObjects=snapshot.underAnalysisMovingObjects;
    }

    private void _storeState(int _currentStateIndex) throws IllegalActionException {
        // TODO Auto-generated method stub
        
        int eventCounter=-1;
        for(DEEvent object: _stateSpace.get(_currentStateIndex).eventQueue){
            eventCounter++;
            NamedObj actor=(NamedObj) object.actor();
            //Store tokens on the ports
            IOPort port=object.ioPort();
            if(port!=null){
                Receiver[][] receiver=port.getReceivers();
                Map<Integer, Token> temp=new TreeMap<Integer, Token>();
                //For each channel, check existence of the token in it. 
                for(int i=0;i<port.getWidth();i++){
                    if(port.hasNewToken(i)){
                        Token token=((ATCReceiver)receiver[i][0]).getToken();
                        temp.put(i,token);
                    }
                }
                // For one event, we check all channels of the port.
                    _stateSpace.get(_currentStateIndex).inputTokens.put(actor.getFullName()+port.getFullName(), temp);
            }
            // End of storing tokens on the ports
            
            if(actor instanceof Airport){
                _stateSpace.get(_currentStateIndex).airportActors.put(eventCounter,new AirportFeilds(
                        ((Airport)actor)._airplanes,((Airport)actor)._inTransit, ((Airport)actor)._transitExpires));
            }
            else if(actor instanceof DestinationAirport) {
                _stateSpace.get(_currentStateIndex).destinationAirportActors.put(eventCounter, new DestinationAirportFields(((DestinationAirport)actor)._inTransit, ((DestinationAirport)actor)._transitExpires, ((DestinationAirport)actor)._called));
            }
            else if(actor instanceof  Track_NR ){
                _stateSpace.get(_currentStateIndex).trackActors.put(eventCounter, new TrackFields(
                        ((Track_NR)actor)._called, ((Track_NR)actor)._inTransit, ((Track_NR)actor)._OutRoute, ((Track_NR)actor)._transitExpires,((Track_NR)actor).generator, ((Track_NR)actor).movingObjectsList));
            }
        }
     // Store status of regions
        for(int i=0;i<regions.size();i++) {
            _stateSpace.get(_currentStateIndex).regionsUnderAnalysis.put(i+1, regions.get(i).underAnalysis);
            for(int j=0;j<regions.get(i).aircraftHasArrived.size();j++)
                _stateSpace.get(_currentStateIndex).isAircraftArrived.add(i+1+","+regions.get(i).aircraftHasArrived.get(j));
        }
        

        
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
            int targetRegionId=Integer.valueOf(parts[0].substring(0,parts[0].split(",")[0].length()))-1;
            String[] travelingMap=parts[0].substring(parts[0].split(",")[0].length()+1).split(",");
            String temp=travelingMap[0];
            temp=temp.substring(1, temp.length()-1);
            String[] temp2=temp.split("/");
            Track_NR track=regions.get(targetRegionId)._containsTrack(regions.get(targetRegionId).tracks,Integer.valueOf(temp2[0]));
            Airport airport=_airportsId.get(((IntToken)track.connectedSourceA.getToken()).intValue());
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
            _LoadAirport(airport,Double.valueOf(temp2[1]),new RecordToken(aircraft));
            counter++;
            }
    }
    


    private void _LoadAirport(Airport airport, Double time, RecordToken aircraft) {
        // TODO Auto-generated method stub
        Map<String, Token> airplane = new TreeMap<String, Token>();
        airplane.put("aircraftId", ((RecordToken)aircraft).get("aircraftId"));
        airplane.put("aircraftSpeed", ((RecordToken)aircraft).get("aircraftSpeed"));
        airplane.put("flightMap", ((RecordToken)aircraft).get("flightMap"));
        airplane.put("priorTrack",((RecordToken)aircraft).get("priorTrack"));
        airplane.put("fuel", ((RecordToken)aircraft).get("fuel"));
        airplane.put("departureTime", new DoubleToken(time));
        airport.putMovingObject(airplane);
        
    }




    private ArrayList<Snapshot> _stateSpace;
    private TimedStatesList _timeReachableStates;
    private HashMap<Snapshot, Integer> _timeReachableStatesMap;
    private Snapshot _currentTimedState;
    private int _currentStateIndex;
    private int _nextStateIndex;
    private boolean _stopAnalysis;
    
    int numOfRegions;
    
    // true if the information has been loaded.
    private boolean _informationLoaded;
    
    /**
     * Arrival time to the traffic network for each aircraft
     */
    private Map<Integer, Double> arrivalTimeToNet;
    
    /**
     * regions. This variable contains tracks of each region.
     */
    private ArrayList<NRRegion> regions;
    
    private int recentlyTimeStVisited=-1;
    
    // To store states generated between two timed transitions
    private HashMap<Snapshot, Integer> tempStates;
    
    private long startTime; 
    
    /**The dimension of the network which is n*n. */
    private int dimension;
    
    /**The dimension of a region: number of the regions are (n/regionDimension)*(n/regionDimension). */
    private int regionDimension;
    

    /**
     * True if a deadlock (livelock) happens in the model. The deadlock happens when at least a moving object runs out of fuel.  
     */
    private boolean deadlockDetected;
    
    /**
     * If the execution time goes more than one houre. 
     */
    private boolean timeOver;
    
    private Map<Integer, String> airplanes;
    
    /**
     * Count the number of all states
     */
    private int _numberOfAllStates;
    
    /**
     * Count the number of timed states
     */
    
    private int _numberOfTimedStates;
    
    public boolean getRegionStatus(int destinationRegionId) {
        // TODO Auto-generated method stub
        int regionId=destinationRegionId-1;
        return regions.get(regionId).underAnalysis;
    }
    


    public boolean nextIsAirport(int nextTrack) {
        // TODO Auto-generated method stub
        if(_airportsId.containsKey(nextTrack))
            return true;
        return false;
    }
    
    public boolean isAirport(int id) {
        if(_airportsId.containsKey(id))
            return true;
        return false;
    }
    

   
   
   public int findRegionofTrack(int trackNum) {
       // TODO Auto-generated method stub
       int numOfRegionsInEachRowOrColumn=dimension/regionDimension;
       int w=trackNum/dimension; // to find the row indexed from 0
       int z=trackNum%dimension; // to find the column indexed from 1

       
       int x=w/regionDimension; // to find row of the region
       int y=z/regionDimension; // to find column of the region
       
       if(z==0) {
           int region=regionDimension;
           if(w%regionDimension==0)
               return (x*numOfRegionsInEachRowOrColumn);
           else if(w%regionDimension!=0)
               return (numOfRegionsInEachRowOrColumn+x*numOfRegionsInEachRowOrColumn);   
       }

       
       if(z%regionDimension==0)
           return x*numOfRegionsInEachRowOrColumn+y;
       else
           return x*numOfRegionsInEachRowOrColumn+y+1;
   }

    
    public String callMe() {
        String x="";
        x+=_currentStateIndex+" ";
        x+=_stateSpace.size();
        return x;
    }

    /**
     * Returns route of a moving object in a region
     * @param planeId
     * @param destinationRegionId
     * @return
     */
    public String getRoute(int planeId, int destinationRegionId) {
        // TODO Auto-generated method stub
        if(regions.get(destinationRegionId-1).travelingAicrafts.containsKey(planeId))
        return regions.get(destinationRegionId-1).travelingAicrafts.get(planeId);
        else 
            return "";
        
    }
    
    
    /**
     * Get departure time of the given moving object from the given region
     * @param planeId
     * @param regionId
     * @return
     */
    public double getDepartureTime(int planeId, int regionId) {
        // TODO Auto-generated method stub
        if(!regions.get(regionId-1).travelingAicrafts.containsKey(planeId))
            return -1;
        String route=regions.get(regionId-1).travelingAicrafts.get(planeId);
        String[] parts=route.split(";");
        String[] parts0=parts[0].split(",");
        String schedule=parts0[parts0.length-1];
        schedule=schedule.substring(1, schedule.length()-1);
        return Double.valueOf(schedule.split("/")[1])+1;
    }
    
    public int getDepartureTrack(int planeId, int regionId) {
        // TODO Auto-generated method stub
        if(regions.get(regionId-1).departureTracks.containsKey(planeId))
            return regions.get(regionId-1).departureTracks.get(planeId);
        else
            return -1;
    }
    
    /**
     * Return true if the given track is in the region.
     * @param regionId
     * @param nextTrack
     * @return
     */
    public boolean isInTheRegion(int regionId, int nextTrack) {
        // TODO Auto-generated method stub
        if(regions.get(regionId-1)._containsTrack( regions.get(regionId-1).tracks, nextTrack)!=null)
            return true;
        return false;
    }

    
    public boolean inputTokensContain(int trackId) {
        
        for(Entry<String, Map<Integer,Token>> entry: _stateSpace.get(_currentStateIndex).inputTokens.entrySet()) {
            for(Entry<Integer, Token> inEntry: entry.getValue().entrySet()){
                if(inEntry.getValue()!=null) {
                    String flightMap=((StringToken)((RecordToken)inEntry.getValue()).get("flightMap")).stringValue();
                    if(!flightMap.equals(""))
                        if(Integer.valueOf(flightMap.split(",")[0].split("/")[0].substring(1))==trackId)
                            return true;
                }
            }
        }
        return false;
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
           deadlockDetected=true;
//           throw new IllegalActionException("deadlock: the airplane "+aircraftWithInformation.get("aircraftId")+ " With route "+aircraftWithInformation.get("flightMap"));
       newAircraft.put("fuel", new DoubleToken(fuel));
        return new RecordToken(newAircraft);
    }
    
    /**
     * We assumed that we will run the model for several input files, for instance input1.txt, input2.txt and for each input
     * file we will get two output files, for ins:outputS1.txt, outputT1.txt and ...
     * We assume that the executive director of MagnfierDirector is SDF, we get its current iteration and based on return an index
     * which is used in the name of the input and output files. As we run the model for three different times at which a change happens,
     * we divid the iteration to 3. 
     * @param number
     * @return
     */
    public int iterationCount(int number) {
        int mod=number%3;
        int corr=number/3;
        
        if(mod==0)
            return corr;
        else
            return (corr+1);
    }
    
    public Token findNeighbors(int trackNum) {
        // TODO Auto-generated method stub
        int northNeighbor=trackNum-dimension;
        if(northNeighbor<0)
            northNeighbor=-1;
        
        int southNighbor=trackNum+dimension;
        if(southNighbor>dimension*dimension) {
            if(trackNum==dimension*dimension)
                southNighbor=-1;
            else
                southNighbor=dimension*dimension+dimension+dimension-1+dimension+(trackNum%dimension);
        }
        
        int eastNeighbor=trackNum+1;
        if(trackNum%dimension==0) {
            eastNeighbor=dimension*dimension+dimension+dimension-1+(trackNum/dimension);
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
    public int returnDimention() {
        int n=0;
        try {
            n = ((IntToken)networkDimension.getToken()).intValue();
        } catch (IllegalActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return n;
    }


    public int stateNumber() {
        // TODO Auto-generated method stub
        return _currentStateIndex;
    }
    
    public int getCurrentState() {
        return _currentStateIndex;
    }
    public int test;
}
