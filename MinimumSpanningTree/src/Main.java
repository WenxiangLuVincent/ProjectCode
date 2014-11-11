package test;

import java.util.*;
import java.io.*;

/* Class to store all payloads of Node */
class Node {
    public int id;
    public double x, y, energy;
    public boolean hasSymmetricMWOE;
    public double minimumWeightOutgoingEdge;                                // the length of MWOE, zero if there is none
    public int nodeOnTheOtherSideOfMWOE;                                    // id of the node on the other side of the MWOE
    public boolean isLeader;                                                // whether this node is leader of its belonging component or not
    public ArrayList<Integer> connectedFriends = new ArrayList<Integer>();  // Store the nodes it is connected to in MST
    public ArrayList<Integer> friends = new ArrayList<Integer>();           // Store the id of other nodes in the same tree as this node, i.e. friends
    public ArrayList<Integer> neighbors = new ArrayList<Integer>();         // Store the id of this node's neighbors (within distance 10)
    public HashMap<Integer, Double> edges = new HashMap<Integer, Double>(); // Store the information of this node's edges in <neighborID, length> pair

    public Node( int id, double x, double y, double energyBudget ) {
        nodeOnTheOtherSideOfMWOE = -1; // initialized to a negative value
        hasSymmetricMWOE = false;
        isLeader = true; // initialized to true, because at level 1 all the nodes are leader of itself
        this.id = id;
        this.x = x;
        this.y = y;
        this.energy = energyBudget;
    }

    /* Find the Minimum Weight Outgoing Edge (MWOE) of this node, if there is no outgoing edge, return zero */
    public double FindMWOE() {
        int iteration = 0;
        double MWOE = 0.0;
        for( int id : neighbors ) {
            if( !friends.contains( id ) ) {
                if( iteration == 0 ) {
                    MWOE = edges.get( id );
                }
                else if( edges.get( id ) < MWOE ) {
                        MWOE = edges.get( id );
                }
                iteration++;
            }
        }

        minimumWeightOutgoingEdge = MWOE;
        if( Math.abs( MWOE - 0.0) > 1e-5 ) { // if it's not zero
            nodeOnTheOtherSideOfMWOE = GetNodeIdOfTheOtherEndOfMWOE( MWOE );
        }

        return MWOE; // if it's not an outgoing node, MWOE equals to zero
    }

    /* Add a new friend into the ArrayList */
    public void addNewFriend( int id ) {
        if( this.id != id && !friends.contains( id ) )
            friends.add( id );
    }

    /* Method to find the key in a map given a value*/
    private int GetNodeIdOfTheOtherEndOfMWOE( double mwoe ) {
        Iterator itr = edges.entrySet().iterator();
        while( itr.hasNext() ) {
            Map.Entry<Integer, Double> pairs = (Map.Entry<Integer, Double>) itr.next();
            if( Math.abs( pairs.getValue() - mwoe ) <= 1e-5 ) { // deal with equivalent of two floating-point values
                return pairs.getKey();
            }
        }
        return -1; // if it's not an outgoing node, i.e. does not has an MWOE
    }

    /* Merge nodes into one component and select a new leader */
    public static void MergeAndSelectNewLeader( ArrayList<Node> nodes, PrintWriter fout ) {
        /* Add new edge */
        HashMap<Double, Integer> symmetricEdges = new HashMap<Double, Integer>();
        ArrayList<Node> arrayListCopy = new ArrayList<Node>( nodes );
        for( Node n : nodes ) {
            if( n.friends.isEmpty() ) { // meaning the level 1 of construction, every node is leader of itself, every MWOE should be added
                n.friends.add( n.nodeOnTheOtherSideOfMWOE );
                if( !(n.hasSymmetricMWOE) ) {// Print out the asymmetric added edges
                    if( nodes.size() == Main.initialNumberOfNodes )
                        fout.println( "added " + n.id + "-" + n.nodeOnTheOtherSideOfMWOE );
                    n.connectedFriends.add( n.nodeOnTheOtherSideOfMWOE );
                    Main.SearchNodeReferenceById( n.nodeOnTheOtherSideOfMWOE, nodes ).connectedFriends.add( n.id );
                }
                else { // Print out the symmetric added edges
                    if( arrayListCopy.contains( n ) ) {
                        if( nodes.size() == Main.initialNumberOfNodes )
                            fout.println( "added " + n.id + "-" + n.nodeOnTheOtherSideOfMWOE );
                        n.connectedFriends.add( n.nodeOnTheOtherSideOfMWOE );
                        Main.SearchNodeReferenceById( n.nodeOnTheOtherSideOfMWOE, nodes ).connectedFriends.add( n.id );
                        arrayListCopy.remove( Main.SearchNodeReferenceById( n.nodeOnTheOtherSideOfMWOE, arrayListCopy ) );
                    }
                }
            }
            else {
                if( n.hasSymmetricMWOE && !(symmetricEdges.containsValue( n.nodeOnTheOtherSideOfMWOE )) )
                    symmetricEdges.put( n.minimumWeightOutgoingEdge, n.id );
            }
        }
        arrayListCopy.clear();

        if( !symmetricEdges.isEmpty() ) {
            Node oneNodeOfNewEdgeToAdd = Main.ConvergeCast( symmetricEdges );
            for( Node n : nodes ) {
                n.hasSymmetricMWOE = false;
            }

            if( oneNodeOfNewEdgeToAdd != null ) {
                oneNodeOfNewEdgeToAdd.friends.add( oneNodeOfNewEdgeToAdd.nodeOnTheOtherSideOfMWOE );
                Main.SearchNodeReferenceById( oneNodeOfNewEdgeToAdd.nodeOnTheOtherSideOfMWOE, Main.nodes ).friends.add( oneNodeOfNewEdgeToAdd.id );
                if( nodes.size() == Main.initialNumberOfNodes )
                    fout.println( "added " + oneNodeOfNewEdgeToAdd.id + "-" + oneNodeOfNewEdgeToAdd.nodeOnTheOtherSideOfMWOE );
                oneNodeOfNewEdgeToAdd.hasSymmetricMWOE = true;
                Main.SearchNodeReferenceById( oneNodeOfNewEdgeToAdd.nodeOnTheOtherSideOfMWOE, Main.nodes ).hasSymmetricMWOE = true;
                oneNodeOfNewEdgeToAdd.connectedFriends.add( oneNodeOfNewEdgeToAdd.nodeOnTheOtherSideOfMWOE );
                Main.SearchNodeReferenceById( oneNodeOfNewEdgeToAdd.nodeOnTheOtherSideOfMWOE, nodes ).connectedFriends.add( oneNodeOfNewEdgeToAdd.id );
            }
        }
        symmetricEdges.clear();

        for( Node n : nodes ) {
            n.isLeader = false;
            if( n.hasSymmetricMWOE && n.id > n.nodeOnTheOtherSideOfMWOE ) {
                if( nodes.size() == Main.initialNumberOfNodes )
                    fout.println( "elected " + n.id );
                n.isLeader = true;
            }
        }


        /* Propagate new friends to all existing friends, thus merge */
        for( Node n : nodes ) {
            for( int i = 0; i < n.friends.size(); i++ ) {
                Node myFriend =  Main.SearchNodeReferenceById( n.friends.get( i ), Main.nodes );
                for( int j = 0; j < myFriend.friends.size(); j++ ) {
                    n.addNewFriend( myFriend.friends.get( j ) );
                    Main.SearchNodeReferenceById( myFriend.friends.get( j ), Main.nodes ).addNewFriend( n.id );
                }
                myFriend.addNewFriend( n.id );
            }
        }


    }

    /* Find all neighbors within distance 10 */
    private boolean CheckNeighbor( int id, double x, double y ) {
        double deltaX = Math.abs( this.x - x );
        double deltaY = Math.abs( this.y - y );
        if( !(deltaX <= 1e-5 && deltaY <= 1e-5) ) { // deal with equivalent of two floating-point values
            double distance = Math.sqrt( Math.pow( deltaX, 2 ) + Math.pow( deltaY, 2 ) ); // Compute the Euclidean distance
            if( distance <= Main.radius ) {
                if( !neighbors.contains( id ) ) {
                    neighbors.add( id );
                    edges.put( id, distance );
                }
                return true;
            }
            return false;
        } else {
            return false;
        }
    }

    /* Broadcast to connected friends */
    public void BroadcastMessagesToConnectedFriends( int initiaterID, PrintWriter fout ) {
        for( int myConnectedFriend : connectedFriends ) {
            if( initiaterID != myConnectedFriend ) {
                double energyCost = this.edges.get( myConnectedFriend ) * 1.2;
                this.energy -= energyCost;
                fout.println( "data from " + this.id + " to " + myConnectedFriend + ", " + "energy: " + this.energy );
                Main.SearchNodeReferenceById(myConnectedFriend, Main.nodes).BroadcastMessagesToConnectedFriends( this.id, fout );
            }
        }
    }

    /* Protocol to simulate "creation" of the network, finding all neighbors */
    public void SendDiscoverMessage( ArrayList<Node> arrayList ) {
        for( Node n : arrayList )
            n.RespondToDiscoverMessage( this.id, this.x, this.y );
    }

    public void RespondToDiscoverMessage( int id, double x, double y ) {
        /* Silently record the neighbor and add the edge if it is within distance 10 */
        CheckNeighbor( id, x, y );
    }
}

/* Class to store the broadcast instructions */
class Instruction {
    public int senderId;
    public Instruction( int senderId ) {
        this.senderId = senderId;
    }
}

public class Main {
    public static double minimalBudget;
    public static int initialNumberOfNodes;
    public static final int radius = 10; // Constant value : distance 10
    public static ArrayList<Node> nodes = new ArrayList<Node>();                           // Store the Node information
    public static ArrayList<Instruction> instructions = new ArrayList<Instruction>();      // Store the broadcast instructions

    public static Node SearchNodeReferenceById( int id, ArrayList<Node> nodes ) {
        for( Node n : nodes ) {
            if( n.id == id )
                return n;
        }
        return null;
    }

    public static void ResetAllNodes( ArrayList<Node> nodes ) {
        for( Node n : nodes ) {
            n.isLeader = true;
            n.nodeOnTheOtherSideOfMWOE = -1;
            n.hasSymmetricMWOE = false;
            n.friends.clear();
            n.connectedFriends.clear();
            n.neighbors.clear();
            n.edges.clear();
        }

        for( Node n : nodes )
            n.SendDiscoverMessage( nodes );
    }

    public static void ConstructMST( ArrayList<Node> nodes, PrintWriter fileOutput ) {
        while( true ) {
            String outputStr = "bs";
            for( Node n : nodes ) {
                if( n.isLeader )
                    outputStr += " " + n.id + ",";
            }
            if( nodes.size() == Main.initialNumberOfNodes )
                fileOutput.println( outputStr.substring( 0, outputStr.length() - 1 ) );

            double sumOfMWOE = 0.0;
           /* Initiates every node to find its MWOE */
            for( Node n : nodes ) {
                sumOfMWOE += n.FindMWOE();
            }
           /* if the sum of MWOE equals zero, meaning the construction of MST is finished */
            if( Math.abs( sumOfMWOE - 0.0 ) <= 1e-5 )
                break;
           /* Find all nodes that have symmetric MWOE */
            for( Node n : nodes ) {
                if( !(n.hasSymmetricMWOE) && (n.id == SearchNodeReferenceById( n.nodeOnTheOtherSideOfMWOE, nodes ).nodeOnTheOtherSideOfMWOE) ) {
                    n.hasSymmetricMWOE = true;
                    SearchNodeReferenceById( n.nodeOnTheOtherSideOfMWOE, nodes ).hasSymmetricMWOE = true;
                }
            }

           /* Merge subtrees to bigger tree and select new leader */
            Node.MergeAndSelectNewLeader( nodes, fileOutput );

            /* One level of construction finished, set all nodes to have no symmetric MWOE */
            for( Node n : nodes )
                n.hasSymmetricMWOE = false;
        }
    }

    public static Node ConvergeCast( Map<Double, Integer> map ) {
        Iterator itr = map.entrySet().iterator();
        Map.Entry<Double, Integer> pairs = (Map.Entry<Double, Integer>) itr.next();
        double minLength = pairs.getKey();
        while( itr.hasNext() ) {
            pairs = (Map.Entry<Double, Integer>) itr.next();
            minLength = Math.min( minLength, pairs.getKey() );
        }

        return SearchNodeReferenceById( map.get( minLength ), nodes );
    }

    public static void main( String[] args ) throws Exception {
        /* Check the command-line parameters */
        if( args.length != 1 ) {
            System.out.println( "Wrong command-line parameters" );
            System.exit( 1 );
        }

        /* Check the existence of input.txt */
        File sourceFile = new File( args[0] );
        if( !sourceFile.exists() ) {
            System.out.println( "Source file " + args[0] + " does not exist!" );
            System.exit( 2 );
        }


        /* Read from the input.txt */
        BufferedReader br = new BufferedReader( new FileReader( sourceFile ) );

        String line;
        minimalBudget = Double.parseDouble( br.readLine() );
        while( (line = br.readLine() ) != null ) {
            if( line.substring( 0, 4 ).equals( "node" ) ) {
                String subStr = line.substring( 5 );
                String[] strArray = subStr.split( "," );
                int nodeId = Integer.parseInt( strArray[0] );
                double x = Double.parseDouble( strArray[1] ),
                        y = Double.parseDouble( strArray[2] ),
                        energyBudget = Double.parseDouble( strArray[3] );
                nodes.add( new Node( nodeId, x, y, energyBudget ) );
            } else if( line.substring( 0, 4 ).equals( "bcst" ) ) {
                String senderIdStr = line.substring( 10 );
                instructions.add( new Instruction( Integer.parseInt( senderIdStr ) ) );
            }
        }
        Main.initialNumberOfNodes = nodes.size();
        br.close();


        /* Find neighbors of each node */
        for( Node n : nodes )
            n.SendDiscoverMessage( nodes );


        /* Create the Minimum Spanning Tree using synchronous GHS algorithm */
        PrintWriter fileOutput = new PrintWriter( new File( "log.txt" ) );
        ConstructMST( Main.nodes, fileOutput );



        /* Execute the broadcast instructions */
        ArrayList<Integer> downNodes = new ArrayList<Integer>(); // Store all nodes that are down
        for( Instruction instr : instructions ) {
            int nodesSize = nodes.size();

            int senderNodeId = instr.senderId;
            if( SearchNodeReferenceById( senderNodeId, nodes ) == null ) {
                continue;
            }
            else {
                SearchNodeReferenceById( senderNodeId, nodes ).BroadcastMessagesToConnectedFriends( senderNodeId, fileOutput );
            }


            // Check if any node is down
            for( Node n : nodes ) {
                if( n.energy < minimalBudget && !downNodes.contains( n.id ) ) {
                    fileOutput.println( "node down " + n.id );
                    downNodes.add( n.id );
                }
            }
            for( int id : downNodes ) {
                Node node = SearchNodeReferenceById( id, nodes );
                if( nodes.contains( node ) )
                    nodes.remove( node );
            }

            if( nodes.size() != nodesSize ) { // Means need to reconstruct the MST
                ResetAllNodes( nodes );
                ConstructMST( nodes, fileOutput );
            }
        }

        fileOutput.close();
    }
}
