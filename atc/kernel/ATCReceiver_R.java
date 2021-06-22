/* A receiver for modeling air traffic control systems.

 Copyright (c) 2015-2016 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

 PT_COPYRIGHT_VERSION_2
 COPYRIGHTENDKEY

 */
package ptolemy.domains.atc.kernel;


import ptolemy.actor.IOPort;
import ptolemy.actor.NoRoomException;
import ptolemy.actor.NoTokenException;
import ptolemy.actor.Receiver;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.domains.atc.kernel.policy1.InsideDirector;
import ptolemy.domains.atc.kernel.policy1.TopLevelDirector;
import ptolemy.domains.atc.lib.ControlArea;
import ptolemy.domains.atc.lib.DestinationAirport_R;
import ptolemy.domains.atc.lib.Track_R;
import ptolemy.domains.de.kernel.DEEvent;
import ptolemy.domains.de.kernel.DEReceiver;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NamedObj;

/** A receiver for modeling air traffic control systems.
 *  This receiver checks the containing actor, and if it can reject inputs,
 *  then uses it to determine whether to accept a token.
 *  @author Maryam Bagheri
 *  @version $Id$
 *  @since Ptolemy II 11.0
 */
public class ATCReceiver_R extends DEReceiver {

    /** Create a new receiver.
     */
    public ATCReceiver_R() {
        super();
    }

    /** Create a new receiver in the specified container with the specified
     *  name.
     *  @param container The container.
     *  @exception IllegalActionException If this actor cannot be contained
     *   by the proposed container (see the setContainer() method).
     */
    public ATCReceiver_R(IOPort container) throws IllegalActionException {
        super(container);
    }

    /** Put the token.
     *  @param token The token
     *  @exception IllegalActionException If the token cannot be put
     *  @exception NoRoomException If there is no room.
     */
    @Override
    public void put(Token token)
            throws IllegalActionException, NoRoomException {
        IOPort port = getContainer();
        if (port != null) {
            NamedObj actor = port.getContainer(); //destination actor
            if(actor instanceof ControlArea)
            {
                
               
//                String boundary=((StringToken)((Track_R)actor).isBoundary.getToken()).stringValue();
//                if(boundary.equals("b")) {
                       Track_R destTrack=((TopLevelDirector)((ControlArea)actor).getExecutiveDirector()).returnDesTrack(token);
    //                   if(destTrack instanceof Rejecting) {
    //                       if (((Rejecting) destTrack).reject(token, null)) {
    //                           throw new NoRoomException(actor,
    //                                   "Rejected input on port " + port.getName());
    //                       }
    //                       else {
                       IOPort receivingPort=destTrack.inputPortList().get(0);
                       Receiver[][] receiver=receivingPort.getReceivers();
                       try {
                           ((ATCReceiver_R)receiver[0][0]).put(token);
                       }
                       catch (NoRoomException e) {
                        // TODO: handle exception
                           throw new NoRoomException(actor,
                                   "Rejected input on port " + receivingPort.getName());
                       }
                       return;

            }
            
            if (actor instanceof Rejecting) {
              //Set time of the destination control area
              
                if(actor instanceof Track_R)
                    ((InsideDirector)((Track_R)actor).getDirector()).setModelTime(((ControlArea)((Track_R)actor).getContainer()).getExecutiveDirector().getModelTime());
                else if (actor instanceof DestinationAirport_R)
                    ((InsideDirector)((DestinationAirport_R)actor).getDirector()).setModelTime(((ControlArea)((DestinationAirport_R)actor).getContainer()).getExecutiveDirector().getModelTime());
                
//                Time time3=((InsideDirector)((Track_R)actor).getDirector()).getModelTime();
                
                //If the source actor is internal boundary and destination is pre-boundary 
                Track_R source=null;
                if(actor instanceof Track_R) {
                    source=((TopLevelDirector)(((ControlArea)(((Track_R)actor).getContainer())).getExecutiveDirector())).returnSourceTrack(token);
                if(actor instanceof DestinationAirport_R)
                    source=((TopLevelDirector)(((ControlArea)(((DestinationAirport_R)actor).getContainer())).getExecutiveDirector())).returnSourceTrack(token);
                
                if(source==null) { //source is a source airport
                    if (((Rejecting) actor).reject(token, port)) {
                        throw new NoRoomException(actor,
                                "Rejected input on port " + port.getName());
                    }
                    super.put(token);
                    return;
                }
                
                
                //If source actor is a pre-boundary actor, and the destination is an internal boundary actor
                if(source.isBoundary.getToken()!=null && ((StringToken)source.isBoundary.getToken()).stringValue().equals("pb") && ((Track_R)actor).isBoundary.getToken()!=null &&
                        ((StringToken)((Track_R)actor).isBoundary.getToken()).stringValue().equals("inb")) {
                    ((TopLevelDirector)(((ControlArea)(((Track_R)actor).getContainer())).getExecutiveDirector())).addTpreToInternalB(((IntToken)source._id).intValue(),false);
                }
                
                //if we are in the parallel execution and a pre-boundary or boundary actor requests
                // to send to a pre-boundary actor
                if(((TopLevelDirector)(((ControlArea)(((Track_R)actor).getContainer())).getExecutiveDirector())).inParallelExecution)
                {
                     if(((Track_R)actor).isBoundary.getToken()!=null &&
                        ((StringToken)((Track_R)actor).isBoundary.getToken()).stringValue().equals("pb"))
                    {
                        int id=((IntToken)((Track_R)actor)._id).intValue();
                     // if the pre-boundary actor has been sent to an internal boundary actor
                        if(((TopLevelDirector)(((ControlArea)(((Track_R)actor).getContainer())).getExecutiveDirector())).containPreb(id))
                        {
                            if(((TopLevelDirector)(((ControlArea)(((Track_R)actor).getContainer())).getExecutiveDirector())).isRejectPhase==true)
                                //if we are in the reject phase, reject the aircraft
                                throw new NoRoomException(actor,
                                        "Rejected input on port " + port.getName());
                        }
                    }
                }
                
                // if source is a pre-boundary but destination is a boundary actor
                if(source.isBoundary.getToken()!=null && ((StringToken)source.isBoundary.getToken()).stringValue().equals("pb") && ((Track_R)actor).isBoundary.getToken()!=null &&
                        ((StringToken)((Track_R)actor).isBoundary.getToken()).stringValue().equals("b"))
                    ((TopLevelDirector)((ControlArea)((InsideDirector)((Track_R)actor).getDirector()).getContainer()).getExecutiveDirector()).boundaryTrack=(Track_R)actor;
                
                // if source is an internal boundary actor and destination is a pre-boundary actor
                if(source.isBoundary.getToken()!=null && ((StringToken)source.isBoundary.getToken()).stringValue().equals("inb") 
                        && ((Track_R)actor).isBoundary.getToken()!=null && ((StringToken)((Track_R)actor).isBoundary.getToken()).stringValue().equals("pb")) 
                {
                    if(((Track_R)actor)._inTransit!=null && !((Track_R)actor)._transitExpires.equals(getModelTime())) {
                        
                        //In this case we are sure that the send will be rejected.
                        if (((Rejecting) actor).reject(token, port)) {
                            throw new NoRoomException(actor,
                                    "Rejected input on port " + port.getName());
                        }
                    }
                    else {
                        //First store the sender as a dependent actor to a pre-boundary actor.
                        ((TopLevelDirector)(((ControlArea)(((Track_R)actor).getContainer())).getExecutiveDirector())).storeDependentActor(((IntToken)source.trackId.getToken()).intValue(),((IntToken)((Track_R)actor).trackId.getToken()).intValue());
                        if(((InsideDirector)((Track_R)actor).getDirector()).isItAcceptPhase==true) {
                            //if the destination actor is full
                            if(((Track_R)actor)._inTransit!=null) {
                                //generate an event to fire the next actor and put it into the parallelEventQueue
                                ((InsideDirector)((Track_R)actor).getDirector()).eventsOfPreboundaries.add(new DEEvent(((Track_R)actor).input, getModelTime(), 1, 0));
                                _tokens.add(token);
                            }
                            else {
                                //make the call value of the next actor true,
                                ((Rejecting) actor).reject(token, port);
                                //create an event to fire the next actor and store it in the parallelEventQueue
//                                ((TopLevelDirector)((ControlArea)actor.getContainer()).getExecutiveDirector())._internalEventQueue.put(new DEEvent((Actor)actor, getModelTime(), 1, 0));
                                ((InsideDirector)((Track_R)actor).getDirector()).eventsOfPreboundaries.add(new DEEvent(((Track_R)actor).input, getModelTime(), 1, 0));
                                _tokens.add(token);
                            }
                        }else {
                            // We are in the reject phase
                            
                                int size=((InsideDirector)((Track_R)actor).getDirector()).eventsOfPreboundaries.size();
                                ((InsideDirector)((Track_R)actor).getDirector()).eventsOfPreboundaries.remove(size-1);
                                _tokens.removeFirst();
                           
                            
                            throw new NoRoomException(actor,
                                    "Rejected input on port " + port.getName());
                        }
                        return;
                    }
                }
                }
                
                //If we are in the parallel execution and the destination actor is pre-boundary actor
                if( (actor instanceof Track_R) && ((TopLevelDirector)(((ControlArea)(((Track_R)actor).getContainer())).getExecutiveDirector())).inParallelExecution)
                {
                    if(((StringToken)((Track_R)actor).isBoundary.getToken()).stringValue().equals("pb"))
                        ((TopLevelDirector)(((ControlArea)(((Track_R)actor).getContainer())).getExecutiveDirector())).updateReqInfoOfDependentActors(((IntToken)((Track_R)actor).trackId.getToken()).intValue(), true);
                }
                
                if (((Rejecting) actor).reject(token, port)) {
                    TopLevelDirector dir=null;
                    if(actor instanceof Track_R)
                        dir=((TopLevelDirector)(((ControlArea)(((Track_R)actor).getContainer())).getExecutiveDirector()));
                    if(actor instanceof DestinationAirport_R)
                        dir=((TopLevelDirector)(((ControlArea)(((DestinationAirport_R)actor).getContainer())).getExecutiveDirector()));
                        
                    if(dir.inParallelExecution)
                        dir.rejected=true;
                    throw new NoRoomException(actor,
                            "Rejected input on port " + port.getName());
                }
                
                
                
            } // end of instance of rejecting
        } //end of port not null
        super.put(token);
    }
    


    /** Put the token without generating the triggering event.
     * @param token The token.
     * @throws IllegalActionException
     */
    public void putToken(Token token) throws IllegalActionException {
        if(token ==null)
            throw new IllegalActionException("token is null");
        _tokens.add(token);
    }
    
    /** Get the first token of the receiver without removing it.
     * @return The token.
     */
    public Token getToken(){
        if (_tokens.isEmpty()) {
            throw new NoTokenException(getContainer(),
                    "No more tokens in the DE receiver.");
        }
        return (Token)_tokens.getFirst();
    }
}
