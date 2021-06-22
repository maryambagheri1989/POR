package ptolemy.domains.atc.kernel;

import ptolemy.actor.util.Time;
import ptolemy.data.Token;

public class DestinationAirportFields {
    public DestinationAirportFields(Token _inTransit, Time _transitExpires, boolean _called) {
        super();
        this._inTransit = _inTransit;
        this._transitExpires = _transitExpires;
        this._called = _called;
    }
    public Token _inTransit;
    public Time _transitExpires;
    public boolean _called;
    


    @Override
    public boolean equals(Object e) {
        if(e instanceof DestinationAirportFields) {
            
            if((((DestinationAirportFields) e)._inTransit!=null && this._inTransit==null)||
                    (((DestinationAirportFields) e)._inTransit==null && this._inTransit!=null))
                return false;
            if((((DestinationAirportFields) e)._inTransit!=null && this._inTransit!=null)
                    && !((DestinationAirportFields) e)._inTransit.equals(this._inTransit))
                    return false;
//                {
//                if(((IntToken)(((RecordToken)((DestinationAirportFields) e)._inTransit).get("aircraftId"))).intValue()!=((IntToken)((RecordToken)this._inTransit).get("aircraftId")).intValue())
//                    return false;
//                if(!((RecordToken)((DestinationAirportFields) e)._inTransit).get("flightMap").equals(((RecordToken)this._inTransit).get("flightMap")))
//                    return false;
//                }
                    

            if((((DestinationAirportFields) e)._transitExpires!=null && this._transitExpires==null)
                    ||(((DestinationAirportFields) e)._transitExpires==null && this._transitExpires!=null))
                return false;
            if((((DestinationAirportFields) e)._transitExpires!=null && this._transitExpires!=null) &&
                    !((DestinationAirportFields) e)._transitExpires.equals(this._transitExpires))
                return false;
            if(((DestinationAirportFields) e)._called!=this._called)
                return false;
        }
        return true;
    }

    
}
