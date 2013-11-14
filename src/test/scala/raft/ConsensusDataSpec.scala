package raft

import akka.testkit._
import org.scalatest._

class ConsensusDataSpec extends RaftSpec with WordSpecLike
    with MustMatchers with BeforeAndAfterEach {

  val probe = TestProbe()
  val nodes = for (i <- List.range(0, 5)) yield TestProbe().ref
  val nextIndices = (for (i <- List.range(0, 5)) yield (TestProbe().ref, 3)).toMap
  val matchIndices = (for (i <- List.range(0, 5)) yield (TestProbe().ref, 0)).toMap

  var state: Meta[Command] = _

  //  state.term.current
  //  state.votes.received
  //  state.requests.pending
  //  state.log.*
  //  state.rsm.execute()
  val testRsm = new TotalOrdering
  override def beforeEach = state = Meta(Term(1), List(), testRsm, nodes)

  "meta" must {
  }

  "term" must {
    "increase term monotonically" in {
      val t = Term(1)
      val t1 = t.nextTerm
      t1 must be(Term(2))
      val t2 = t1.nextTerm
      t2 must be(Term(3))
    }
    "compare to terms against each other" in {
      val t1 = Term(1)
      val t2 = Term(2)
      t1 must be < t2
    }
    "find the max term given two terms" in {
      val t1 = Term(1)
      val t2 = Term(2)
      Term.max(t1, t2) must be(t2)
      Term.max(t2, t1) must be(t2)
    }
  }

  "votes" must {
    "keep track of votes received" in {
      val v = Votes()
      val v2 = v.gotVoteFrom(probe.ref)
      v2.received must have length (1)
    }
    "check if majority votes received" in {
      val v = Votes(received = List(probe.ref, probe.ref))
      v.majority(5) must be(false)
      v.majority(3) must be(true)
    }
    "keep at most one vote for a candidate per term" in {
      val thisMeta = Meta(
        term = Term(1),
        votes = Votes(votedFor = Some(probe.ref)),
        log = List(),
        nodes = nodes,
        rsm = testRsm,
        requests = Requests())
      state.votes = state.votes.vote(probe.ref)
      state must be(thisMeta)
      state.votes.vote(TestProbe().ref) must be(Votes(votedFor = Some(probe.ref)))
    }
  }

  "client requests" must {
    "store pending requests" in {
      val r1 = Requests()
      val request = ClientRequest(ClientCommand(100, "add"))
      val ref = ClientRef(probe.ref, 100)
      val r2 = r1.add(ref, request)
      r2.pending must contain key (ref)
    }
    "increase append success count per request" in {
      val request = ClientRequest(ClientCommand(100, "add"))
      val ref = ClientRef(probe.ref, 100)
      val r1 = Requests(Map(ref -> request))
      val r2 = r1.tick(ref)
      r2.pending(ref).successes must be(1)
    }
    "check if majority has been reached per request" in {
      val request = ClientRequest(ClientCommand(100, "add"), 2)
      val ref = ClientRef(probe.ref, 100)
      val r1 = Requests(Map(ref -> request))
      r1.majority(ref, 5) must be(false)
      r1.majority(ref, 3) must be(true)
    }
    "be able to delete requests that have been replied to" in {
      val request = ClientRequest(ClientCommand(100, "add"), 2)
      val ref = ClientRef(probe.ref, 100)
      val r1 = Requests(Map(ref -> request))
      val r2 = r1.remove(ref)
      r2.pending must not contain key(ref)
    }
  }

  "a log" must {
    "maintain a next index for each follower" in {
      //      val nodes = for (i <- List.range(0, 5)) yield (TestProbe().ref, 0)
      //      val log = Log(entries = List(LogEntry("a", 1), LogEntry("b", 2)), nodes.toMap)
      //      for (value <- log.nextIndex.values) yield value must be(3)
      pending
    }
    "decrement the next index for a follower if older log entries must be passed" in {
      val log = Log(entries = List(LogEntry("a", 1), LogEntry("b", 2)),
        nextIndices, matchIndices)
      log.decrementNextFor(nextIndices.head._1).nextIndex(nextIndices.head._1) must be(2)
    }
    "set the next index for a follower based on the last log entry sent" in {
      val log = Log(entries = List(LogEntry("a", 1), LogEntry("b", 2)),
        nextIndices, matchIndices)
      log.resetNextFor(nextIndices.head._1, 60).nextIndex(nextIndices.head._1) must be(60)
    }
    "increase match index monotonically" in {
      val log = Log(
        entries = List(LogEntry("a", 1), LogEntry("b", 2)),
        nextIndex = nextIndices,
        matchIndex = matchIndices)
      log.matchFor(matchIndices.head._1).matchIndex(matchIndices.head._1) must be(1)
    }
    "set match index to specified value" in {
      val log = Log(
        entries = List(LogEntry("a", 1), LogEntry("b", 2)),
        nextIndex = nextIndices,
        matchIndex = matchIndices)
      log.matchFor(matchIndices.head._1, Some(100)).matchIndex(matchIndices.head._1) must be(100)
    }
    "override apply to initialise with appropiate next and match indices" in {
      val entries = List(LogEntry("a", 1), LogEntry("b", 2))
      val nodes = for (n <- List.range(0, 5)) yield TestProbe().ref
      Log(nodes, entries).nextIndex(nodes(0)) must be(2)
    }
  }

  "replicated state machine" must {
    "apply commands to a generic state machine" in (pending)
    "keep track of the log index of the last command applied to the state machine" in (pending)
  }

  "state factory" must {
    "create a state object from file" in (pending)
    "persist a state object to file" in (pending)
  }
}