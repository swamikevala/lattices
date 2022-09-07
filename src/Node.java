public class Node implements Comparable<Node>{
	
	private int localId;
	private int globalId;
	private Rational rate;
	private Rational weight;

	public Node(int localId, int globalId, Rational weight, Rational rate) {
		this.localId = localId;
		this.globalId = globalId;
		this.weight = weight;
		this.rate = rate;
	}
	
	public int getLocalId() {
		return localId;
	}

	public void setLocalId(int localId) {
		this.localId = localId;
	}

	public int getGlobalId() {
		return globalId;
	}

	public void setGlobalId(int globalId) {
		this.globalId = globalId;
	}

	public Rational getRate() {
		return rate;
	}
	
	public void setRate(Rational rate) {
		this.rate = rate;
	}

	public Rational getWeight() {
		return weight;
	}
	
	public void setWeight(Rational weight) {
		this.weight = weight;
	}

	public Node clone() {
		return new Node(this.localId, this.globalId, this.weight, this.rate);
	}

	// clone node but with given ids 
	public Node clone(int localId, int globalId) {
		Node n = this.clone();
		n.setLocalId(localId);
		n.setGlobalId(globalId);
		return n;
	}
	
	@Override
	public int compareTo(Node other) {
		if ( weight.lt(other.weight) ){
			return -1;
		} else if ( weight.gt(other.weight) ){
			return 1;
		} else {
			return 0;
		}
	}
	
}
