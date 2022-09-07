import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.jgrapht.Graph;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.layout.algorithms.CircleLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.KKLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.SugiyamaLayoutAlgorithm;

public class Lattice {
	
	private Graph<Node, DefaultEdge> graph;
	private Map<Integer, Node> localLookup;
	private Map<Integer, Node> globalLookup;
	private Lattice prevSubLattice;
	private Set<Integer> spawnerGlobalIds;
	
	// create initial single node lattice 
	public Lattice() {
		this.graph = new SimpleDirectedGraph<Node, DefaultEdge>(DefaultEdge.class);
		Node base = new Node(1, 1, Rational.ONE, Rational.ONE);
		this.graph.addVertex(base);
		this.localLookup = new HashMap<Integer, Node>();
		this.globalLookup = new HashMap<Integer, Node>();
		this.localLookup.put(1, base);
		this.globalLookup.put(1, base);
		this.spawnerGlobalIds = new HashSet<Integer>();
		this.prevSubLattice = this;
	}
	
	// copy and then grow the current subLattice and add it to the main lattice, identifying nodes of the same weight
	public Lattice grow() {

		Lattice growthSubLattice = deepCopy(prevSubLattice);
		int subGraphMaxLocalId = getMaxId(growthSubLattice, true);
		int subGraphMaxGlobalId = getMaxId(growthSubLattice, false);
		int subGraphNextLocalId = subGraphMaxLocalId + 1;
		int subGraphNextGlobalId = subGraphMaxGlobalId + 1;
		
		// update the weights and upshift global ids
		for ( Node node : growthSubLattice.getGraph().vertexSet() ) {

			Rational weight = node.getWeight();
			Rational rate = node.getRate();
			Rational newWeight = weight.add(rate);
			node.setWeight(newWeight); 
			node.setGlobalId(subGraphNextGlobalId);
			subGraphNextGlobalId++;
		}
		
		// spawn new node and connect to base node
		Node baseNode = getNodeById(growthSubLattice, 1, true);
		Rational newRate = baseNode.getWeight().reciprocal();
		Node newNode = new Node(subGraphNextLocalId, subGraphNextGlobalId, newRate, newRate); // weight and rate are the same for new node
		growthSubLattice.getGraph().addVertex(newNode);
		growthSubLattice.getGraph().addEdge(baseNode, newNode);
 
		// connect spawner nodes to new spawned node IF they are in the subLattice
		for (Integer id : prevSubLattice.spawnerGlobalIds) {
			Node spawnerNode = growthSubLattice.globalLookup.get(id);
			growthSubLattice.getGraph().addEdge(spawnerNode, newNode);
		}
		
		// identify non-base unity rate nodes (for spawning on next growth iteration)
		growthSubLattice.spawnerGlobalIds.clear();
		for (Node node : growthSubLattice.getGraph().vertexSet()) {
			if ( node.getLocalId() > 1 && node.getWeight().fractional().eq(Rational.ZERO) ) { 
				growthSubLattice.spawnerGlobalIds.add(node.getGlobalId());
			}
		}
		
		// merge graphs (by copying nodes and edges)
		Lattice grownLattice = deepUnion(growthSubLattice);
		
		this.globalLookup = grownLattice.getGlobalLookup();
		this.localLookup = grownLattice.getLocalLookup();
		this.graph = grownLattice.getGraph();
		this.spawnerGlobalIds = grownLattice.getSpawnerGlobalIds();
		
		this.prevSubLattice = growthSubLattice;
		
		return grownLattice;
	}
	
	
	// make a deep copy of the main lattice and union it with the grown sublattice graph 
	public Lattice deepUnion(Lattice subLattice) {
		
		// first make a deep copy of the main lattice
		Lattice unionLattice = deepCopy(this);
		Graph<Node, DefaultEdge> unionGraph = unionLattice.getGraph();
		Graph<Node, DefaultEdge> subGraph = subLattice.getGraph();
		
		int unionGraphMaxGlobalId = getMaxId(unionLattice, false);
		int unionGraphNextGlobalId = unionGraphMaxGlobalId + 1;
		
		// next copy the structure from the sub lattice (merging nodes having matching weights)
		int matchId = 0;
		Node copyNode;
		for ( Node node : subGraph.vertexSet() ) {
			// use global ids now
			matchId = getNodeIdByWeight(unionLattice, node.getWeight(), false);
			if ( matchId == 0 ){
				// new weight, so add node
				copyNode = node.clone(node.getLocalId(), unionGraphNextGlobalId);
				unionGraph.addVertex(copyNode);
				unionLattice.localLookup.put(node.getLocalId(), copyNode);
				unionLattice.globalLookup.put(unionGraphNextGlobalId, copyNode);
				unionGraphNextGlobalId++;
			}
		}
		
		// edges 
		Rational aWeight;
		Rational bWeight;
		int aGlobalId;
		int bGlobalId;
		
		for (Node a : subGraph.vertexSet()) {
			for (Node b : subGraph.vertexSet()) {
				if (subGraph.containsEdge(a, b)) {
					aWeight = a.getWeight();
					bWeight = b.getWeight();
					aGlobalId = getNodeIdByWeight(unionLattice, aWeight, false);
					bGlobalId = getNodeIdByWeight(unionLattice, bWeight, false);
					unionGraph.addEdge(unionLattice.globalLookup.get(aGlobalId), unionLattice.globalLookup.get(bGlobalId));
				}
			}
		}
		unionLattice.setGraph(unionGraph);
		return unionLattice;
	}
	
	
	// returns a deep copy of the given lattice
	public static Lattice deepCopy(Lattice lattice){
		
		Lattice copyLattice = new Lattice();
		Graph<Node, DefaultEdge> srcGraph = lattice.getGraph();

		Map<Integer, Node> newLocalLookup = new HashMap<Integer, Node>();
		Map<Integer, Node> newGlobalLookup = new HashMap<Integer, Node>();

		Graph<Node, DefaultEdge> copyGraph = new SimpleDirectedGraph<Node, DefaultEdge>(DefaultEdge.class);
		for (Node n : srcGraph.vertexSet()) {
			Node copyNode = n.clone();
			copyGraph.addVertex(copyNode);
			newLocalLookup.put(copyNode.getLocalId(), copyNode);
			newGlobalLookup.put(copyNode.getGlobalId(), copyNode);
		}
		// edges - surely there must be an easier way to do this!
		for (Node a : srcGraph.vertexSet()) {
			for (Node b : srcGraph.vertexSet()) {
				if (srcGraph.containsEdge(a, b)) {
					copyGraph.addEdge(newGlobalLookup.get(a.getGlobalId()), newGlobalLookup.get(b.getGlobalId()));
				}
			}
		}

		copyLattice.setGraph(copyGraph);
		copyLattice.setLocalLookup(newLocalLookup);
		copyLattice.setGlobalLookup(newGlobalLookup);
		copyLattice.setSpawnerGlobalIds(lattice.getSpawnerGlobalIds());
		copyLattice.setPrevSubLattice(lattice.getPrevSubLattice());
		
		return copyLattice;
	}
	
	
	public static Node getNodeById(Lattice lattice, int id, boolean local) {
		for ( Node node : lattice.getGraph().vertexSet() ) {
			if ( local ) {
				if ( node.getLocalId() == id ) return node;
			} else {
				if ( node.getGlobalId() == id ) return node;
			}
		}
		throw new IllegalArgumentException("Node id not found in the graph");
	}
	
	public static int getMaxId(Lattice lattice, boolean local){
		int max = 0;
		for ( Node n : lattice.getGraph().vertexSet() ) {
			if ( local ) {
				if ( n.getLocalId() > max ) max = n.getLocalId();
			} else {
				if ( n.getGlobalId() > max ) max = n.getGlobalId();
			}
		}
		return max;
	}
	
	// returns 0 if no node of the given weight found in graph
	public static int getNodeIdByWeight(Lattice lattice, Rational weight, boolean local) {
		for (Node node : lattice.getGraph().vertexSet()) {
			if ( node.getWeight().eq(weight)) {
				if ( local ) {
					return node.getLocalId();
				} else {
					return node.getGlobalId();
				}
			}
		}
		return 0;
	}
	
	public Graph<Node, DefaultEdge> getGraph() {
		return graph;
	}

	public void setGraph(Graph<Node, DefaultEdge> graph) {
		this.graph = graph;
	}

	public Set<Integer> getSpawnerGlobalIds() {
		return spawnerGlobalIds;
	}

	public void setSpawnerGlobalIds(Set<Integer> spawnerGlobalIds) {
		this.spawnerGlobalIds = spawnerGlobalIds;
	}
	
	public Map<Integer, Node> getLocalLookup() {
		return localLookup;
	}

	public void setLocalLookup(Map<Integer, Node> localLookup) {
		this.localLookup = localLookup;
	}

	public Map<Integer, Node> getGlobalLookup() {
		return globalLookup;
	}

	public void setGlobalLookup(Map<Integer, Node> globalLookup) {
		this.globalLookup = globalLookup;
	}

	public Lattice getPrevSubLattice() {
		return prevSubLattice;
	}

	public void setPrevSubLattice(Lattice prevSubLattice) {
		this.prevSubLattice = prevSubLattice;
	}

	public void draw() {
		VisualizationViewer<Node, DefaultEdge> vv =
        VisualizationViewer.builder(graph)
            .viewSize(new Dimension(700, 700))
            .layoutAlgorithm(new SugiyamaLayoutAlgorithm<>())
            .build();
    
		vv.getRenderContext().setVertexLabelFunction(v -> v.getWeight().toString());
		vv.getRenderContext().setVertexFillPaintFunction(v -> {
			Color c;
			if (v.getGlobalId() == 1) {
				c = Color.GREEN;
			} else if (v.getWeight().lt(Rational.ZERO)) {
				c = Color.RED;
			} else if (v.getWeight().gt(Rational.ZERO)) {
				c = Color.BLUE;
			} else {
				c = Color.WHITE;
			}
			return c;
		});

	    // create a frame to hold the graph visualization
	    final JFrame frame = new JFrame();
	    frame.getContentPane().add(vv.getComponent());
	    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	    frame.pack();
	    frame.setVisible(true);
	}
	
}

