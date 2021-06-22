
package ptolemy.domains.atc.lib;

import ptolemy.actor.Actor;
import ptolemy.actor.Director;
import ptolemy.actor.TypedCompositeActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.util.CausalityInterface;
import ptolemy.actor.util.CausalityInterfaceForComposites;
import ptolemy.actor.util.Dependency;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.vergil.icon.EditorIcon;
import ptolemy.vergil.kernel.attributes.RectangleAttribute;
import ptolemy.vergil.kernel.attributes.ResizablePolygonAttribute;



public class ControlArea extends TypedCompositeActor{
   
    
    public ControlArea(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
        super(container, name);
        // TODO Auto-generated constructor stub
        areaId=new Parameter(this, "areaId");
        areaId.setTypeEquals(BaseType.INT);
        String n=name.replace("ControlArea", "");
        if(n.equals(""))
            areaId.setExpression("1");
        else {
            areaId.setExpression(n);
        }
       
        setClassName("ptolemy.domains.atc.lib.ControlArea");
        
        input1 = new TypedIOPort(this, "input1", true, false);
        input2 = new TypedIOPort(this, "input2", true, false);
        input3 = new TypedIOPort(this, "input3", true, false);
        input4 = new TypedIOPort(this, "input4", true, false);
        input5 = new TypedIOPort(this, "input5", true, false);
        input6 = new TypedIOPort(this, "input6", true, false);
        input7 = new TypedIOPort(this, "input7", true, false);
        input8 = new TypedIOPort(this, "input8", true, false);
        input9 = new TypedIOPort(this, "input9", true, false);
        
        nInput1 = new TypedIOPort(this, "nInput1", true, false);
        StringAttribute cardinality = new StringAttribute(nInput1,
                "_cardinal");
        cardinality.setExpression("East");
        
        nInput2 = new TypedIOPort(this, "nInput2", true, false);
        cardinality = new StringAttribute(nInput2,
                "_cardinal");
        cardinality.setExpression("East");
        
        nInput3 = new TypedIOPort(this, "nInput3", true, false);
        cardinality = new StringAttribute(nInput3,
                "_cardinal");
        cardinality.setExpression("East");
        
        nInput4 = new TypedIOPort(this, "nInput4", true, false);
        cardinality = new StringAttribute(nInput4,
                "_cardinal");
        cardinality.setExpression("East");
        
        nInput5 = new TypedIOPort(this, "nInput5", true, false);
        cardinality = new StringAttribute(nInput5,
                "_cardinal");
        cardinality.setExpression("East");
        
        nInput6 = new TypedIOPort(this, "nInput6", true, false);
        cardinality = new StringAttribute(nInput6,
                "_cardinal");
        cardinality.setExpression("East");
        
        nInput7 = new TypedIOPort(this, "nInput7", true, false);
        cardinality = new StringAttribute(nInput7,
                "_cardinal");
        cardinality.setExpression("East");
        
        nInput8 = new TypedIOPort(this, "nInput8", true, false);
        cardinality = new StringAttribute(nInput8,
                "_cardinal");
        cardinality.setExpression("East");
        
        nInput9 = new TypedIOPort(this, "nInput9", true, false);
        cardinality = new StringAttribute(nInput9,
                "_cardinal");
        cardinality.setExpression("East");
        
        
        output1 = new TypedIOPort(this, "output1", false, true);
        output1.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(output1,
                "_cardinal");
        cardinality.setExpression("SOUTH");
        
        output2 = new TypedIOPort(this, "output2", false, true);
        output2.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(output2,
                "_cardinal");
        cardinality.setExpression("SOUTH");
        
        output3 = new TypedIOPort(this, "output3", false, true);
        output3.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(output3,
                "_cardinal");
        cardinality.setExpression("SOUTH");
        
        output4 = new TypedIOPort(this, "output4", false, true);
        output4.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(output4,
                "_cardinal");
        cardinality.setExpression("SOUTH");
        
        output5 = new TypedIOPort(this, "output5", false, true);
        output5.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(output5,
                "_cardinal");
        cardinality.setExpression("SOUTH");
        
        output6 = new TypedIOPort(this, "output6", false, true);
        output6.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(output6,
                "_cardinal");
        cardinality.setExpression("SOUTH");
        
        output7 = new TypedIOPort(this, "output7", false, true);
        output7.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(output7,
                "_cardinal");
        cardinality.setExpression("SOUTH");
        
        output8 = new TypedIOPort(this, "output8", false, true);
        output8.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(output8,
                "_cardinal");
        cardinality.setExpression("SOUTH");
        
        output9 = new TypedIOPort(this, "output9", false, true);
        output9.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(output9,
                "_cardinal");
        cardinality.setExpression("SOUTH");
        
        nOutput1 = new TypedIOPort(this, "nOutput1", false, true);
        nOutput1.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(nOutput1,
                "_cardinal");
        cardinality.setExpression("NORTH");
        
        nOutput2 = new TypedIOPort(this, "nOutput2", false, true);
        nOutput2.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(nOutput2,
                "_cardinal");
        cardinality.setExpression("NORTH");
        
        nOutput3 = new TypedIOPort(this, "nOutput3", false, true);
        nOutput3.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(nOutput3,
                "_cardinal");
        cardinality.setExpression("NORTH");
        
        nOutput4 = new TypedIOPort(this, "nOutput4", false, true);
        nOutput4.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(nOutput4,
                "_cardinal");
        cardinality.setExpression("NORTH");
        
        nOutput5 = new TypedIOPort(this, "nOutput5", false, true);
        nOutput5.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(nOutput5,
                "_cardinal");
        cardinality.setExpression("NORTH");
        
        nOutput6 = new TypedIOPort(this, "nOutput6", false, true);
        nOutput6.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(nOutput6,
                "_cardinal");
        cardinality.setExpression("NORTH");
        
        nOutput7 = new TypedIOPort(this, "nOutput7", false, true);
        output7.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(nOutput7,
                "_cardinal");
        cardinality.setExpression("NORTH");
        
        nOutput8 = new TypedIOPort(this, "nOutput8", false, true);
        nOutput8.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(nOutput8,
                "_cardinal");
        cardinality.setExpression("NORTH");
        
        nOutput9 = new TypedIOPort(this, "nOutput9", false, true);
        nOutput9.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(nOutput9,
                "_cardinal");
        cardinality.setExpression("NORTH");
        
        
        
        EditorIcon node_icon = new EditorIcon(this, "_icon");

        //rectangle
        _rectangle = new RectangleAttribute(node_icon, "_rectangleShape");
        _rectangle.centered.setToken("true");
        _rectangle.width.setToken("90");
        _rectangle.height.setToken("90");
        _rectangle.rounding.setToken("10");
        _rectangle.lineColor.setToken("{0.0, 0.0, 0.0, 1.0}");
        _rectangle.fillColor.setToken("{0.8,0.8,1.0,1.0}");

        _shape = new ResizablePolygonAttribute(node_icon, "_airplaneShape");
        _shape.centered.setToken("true");
        _shape.width.setToken("40");
        _shape.height.setToken("40");
        _shape.vertices.setExpression("{-194.67321,2.8421709e-14, -70.641958,53.625, "
                + "-60.259688,46.70393, -36.441378,32.34961, -31.736508,30.17602, 7.7035221,11.95523, "
                + "5.2088921,44.90709,-11.387258,54.78122,-15.926428,57.48187,-39.110778,71.95945, "
                + "-54.860708,81.15624,-72.766958,215.09374,-94.985708,228.24999,-106.51696,107.31249, "
                + "-178.04821,143.99999,-181.89196,183.21874,-196.42321,191.84374,-207.51696,149.43749, "
                + "-207.64196,149.49999,-238.45446,117.96874,-223.57946,109.96874,-187.95446,126.87499, "
                + "-119.67321,84.43749,-217.36071,12.25,-194.67321,2.8421709e-14}");
        _shape.fillColor.setToken("{1.0, 1.0, 1.0, 1.0}");
        
    }
    
    
    
    
    /** The input ports*/
    public TypedIOPort input1;
    public TypedIOPort input2;
    public TypedIOPort input3;
    public TypedIOPort input4;
    public TypedIOPort input5;
    public TypedIOPort input6;
    public TypedIOPort input7;
    public TypedIOPort input8;
    public TypedIOPort input9;
    
    public TypedIOPort nInput1;
    public TypedIOPort nInput2;
    public TypedIOPort nInput3;
    public TypedIOPort nInput4;
    public TypedIOPort nInput5;
    public TypedIOPort nInput6;
    public TypedIOPort nInput7;
    public TypedIOPort nInput8;
    public TypedIOPort nInput9;

    /** The output ports. */
    public TypedIOPort output1;
    public TypedIOPort output2;
    public TypedIOPort output3;
    public TypedIOPort output4;
    public TypedIOPort output5;
    public TypedIOPort output6;
    public TypedIOPort output7;
    public TypedIOPort output8;
    public TypedIOPort output9;
    
    public TypedIOPort nOutput1;
    public TypedIOPort nOutput2;
    public TypedIOPort nOutput3;
    public TypedIOPort nOutput4;
    public TypedIOPort nOutput5;
    public TypedIOPort nOutput6;
    public TypedIOPort nOutput7;
    public TypedIOPort nOutput8;
    public TypedIOPort nOutput9;
//    
    @Override
    public boolean prefire() throws IllegalActionException {
            boolean result = getDirector().prefire();
            return result;
    }
    
    @Override
    public CausalityInterface getCausalityInterface() {
        return _causalityInterface;
    }
        
    private CausalityInterface _causalityInterface
            = new BreakCausalityInterfaceForComposites(this,
            getExecutiveDirector().defaultDependency());
    
    public class BreakCausalityInterfaceForComposites
            extends CausalityInterfaceForComposites {

        public BreakCausalityInterfaceForComposites(Actor actor, Dependency defaultDependency) {
            super(actor, defaultDependency);
        }
    }
    
    

    

    
    @Override
    public void fire() throws IllegalActionException {
        // TODO Auto-generated method stub
        _director=getDirector();
        _director.fire();
        

    }
    
    @Override
    public boolean postfire() throws IllegalActionException {
            boolean result = true;
            result = result && getDirector().postfire();
            return result;
    }


    public Parameter areaId;
    private RectangleAttribute _rectangle;
    private ResizablePolygonAttribute _shape;
    private Director _director;
    private String[] _labels = { "aircraftId", "aircraftSpeed", "flightMap",
            "priorTrack", "fuel" };
    private Type[] _types = { BaseType.INT, BaseType.INT,
           BaseType.STRING, BaseType.INT, BaseType.DOUBLE };
}
