package ptolemy.domains.atc.kernel;


import ptolemy.actor.util.Time;
import ptolemy.data.Token;
import ptolemy.domains.atc.lib.ObjectsList;
import ptolemy.domains.atc.lib.ObjectsList.MovingObjectCell;

public class TrackFields {
    
    public TrackFields(boolean _called, Token _inTransit, int _OutRoute, Time _transitExpires, boolean _genMode
            ,ObjectsList objects) {
        super();
        this.called = _called;
        this.inTransit = _inTransit;
        this.OutRoute = _OutRoute;
        this.transitExpires = _transitExpires;
        this.genMode=_genMode;
        this.movingObjectsList=new ObjectsList();
        
        if(!objects.isEmpty()) {
            MovingObjectCell temp=objects.head;
            while(temp!=null) {
                this.movingObjectsList.add(temp.movingObject, temp.timeOfDeparting, temp.departureDirection);
                temp=temp.next;
            }
        }
//        this.movingObject=_movingObject;
    }
    
    public TrackFields(boolean _called, Token _inTransit, int _OutRoute, Time _transitExpires) {
//      , MovingObjectCell _movingObject) {
      super();
      this.called = _called;
      this.inTransit = _inTransit;
      this.OutRoute = _OutRoute;
      this.transitExpires = _transitExpires;
      this.movingObjectsList=new ObjectsList();
  }
  


    
    @Override
    public boolean equals(Object e) {
        if(e instanceof TrackFields) {
            if((((TrackFields) e).inTransit!=null && this.inTransit==null)||(((TrackFields) e).inTransit==null && this.inTransit!=null))
                return false;
            if((((TrackFields) e).inTransit!=null && this.inTransit!=null) &&
                    !((TrackFields) e).inTransit.equals(this.inTransit))     
            if((((TrackFields) e).transitExpires!=null && this.transitExpires==null)||(((TrackFields) e).transitExpires==null && this.transitExpires!=null))
                return false;
            if((((TrackFields) e).transitExpires!=null && this.transitExpires!=null) &&
                    !((TrackFields) e).transitExpires.equals(this.transitExpires))
                return false;
            if(((TrackFields) e).called!=this.called)
                return false;
            if(((TrackFields) e).OutRoute!=this.OutRoute)
                return false;
            if(((TrackFields) e).genMode!=this.genMode)
                return false;
            
        }
        return true;
    }
    

    
    public boolean called;
    public Token inTransit;
    public int OutRoute;
    public Time transitExpires;
    public boolean genMode;
    
    public ObjectsList movingObjectsList;

    
}
