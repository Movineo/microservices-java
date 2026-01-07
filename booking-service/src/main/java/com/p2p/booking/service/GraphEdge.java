package com.p2p.booking.service;

/**
 * Represents an edge (road segment) in the graph
 *
 * @param destination The destination vertex
 * @param weight Travel time in minutes
 */
public record GraphEdge(GraphVertex destination, double weight) {

}
