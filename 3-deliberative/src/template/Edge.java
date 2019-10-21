package template;

public class Edge<T> implements Comparable<Edge<T>> {
	T a;
	T b;
	double weight;
	
	public Edge(T a, T b, double weight) {
		this.a = a;
		this.b = b;
		this.weight = weight;
	}

	@Override
	public int compareTo(Edge<T> o) {
		// Returns a negative integer, zero, or a positive integer as this object is
		// less than, equal to, or greater than the specified object.
		if(this.weight < o.weight)
			return -1;
		else if(this.weight > o.weight)
			return 1;
		else {
			return 0;	
		}
	}

	public String toString() {
		return "From " + this.a.toString() + " to " + this.b.toString() + " with cost " + this.weight;
	}
}