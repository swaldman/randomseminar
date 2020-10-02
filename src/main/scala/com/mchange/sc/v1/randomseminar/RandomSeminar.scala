package com.mchange.sc.v1.randomseminar

import scala.swing._
import scala.swing.event.{ButtonClicked,EditDone,Key,KeyPressed,KeyTyped}
import scala.swing.BorderPanel.Position._

import scala.collection._

import java.awt.Color
import java.awt.Font.{BOLD,PLAIN,SANS_SERIF,MONOSPACED}
import javax.swing.border.EmptyBorder


import scala.concurrent.duration._

import com.mchange.sc.v2.concurrent.Scheduler

object RandomSeminar extends SimpleSwingApplication {

  // implicit val StringOrdering =

  val ChromeBackground = new Color(0xEE, 0xEE, 0xFF)
  val SelectedChrome   = new Color(0xCC, 0xCC, 0xFF)
  val Amber            = new Color(250, 190, 0)

  val DefaultLimitMillis = 120000L

  val FirstWarningFraction  = 0.75d
  val SecondWarningFraction = 0.9d

  final case class Participant( name : String, spokenYet : Boolean )

  val entropy = new java.security.SecureRandom

  val participants = mutable.TreeSet.empty[Participant]( Ordering.by( p => ( p.name, p.spokenYet ) ) )

  // interacted with only by the Swing dispatch Thread
  var currentParticipantName : Option[String] = None
  var sessionElapsedMillis   : Long           = 0L
  var lastTickMillis         : Long           = 0L
  var isPaused               : Boolean        = true
  var sessionLimitMillis     : Long           = DefaultLimitMillis



  val TitleLabel = new Label {
    text = "Manage Participants"
    font = new Font(SANS_SERIF, BOLD, 24)
    opaque = false
    xAlignment = Alignment.Left
  }
  val EntryField = new TextField
  val EntryPanel = new BorderPanel {
    val EntryLabel = new Label {
      text = "Enter participant name:"
      font = new Font(SANS_SERIF, BOLD, 12)
      xAlignment = Alignment.Left
    }
    layout( EntryLabel ) = North
    layout( EntryField ) = Center
    opaque = false
  }

  val ParticipantRenderer = new ListView.Renderer[Participant] {
    def componentFor(list : ListView[_ <: Participant], isSelected : Boolean, focused : Boolean, a : Participant, index : Int) : Component = {
      new Label {
        text = a.name
        foreground = if (a.spokenYet) Color.lightGray else Color.black
        font = new Font( SANS_SERIF, BOLD, 12 )
        xAlignment = Alignment.Left

        if ( isSelected ) {
          opaque = true
          foreground = Color.black
          background = SelectedChrome
        }
      }
    }
  }

  val ParticipantList = new ListView( Seq.empty[Participant] ) {

    opaque = false
    border = new EmptyBorder(8,8,8,8)
    renderer = ParticipantRenderer
    minimumSize = new Dimension( 500, 300 )
    preferredSize = minimumSize

    def refresh() : Unit = {
      if ( listData.toSet != participants.toSet ) listData = participants.toSeq
    }
  }
  val EntryTab = new BorderPanel {
    border = new EmptyBorder(8,8,8,8)
    background = ChromeBackground
    layout( TitleLabel )      = North
    layout( ParticipantList ) = Center
    layout( EntryPanel )      = South
  }
  val CurrentSpeakerTab = new BorderPanel {

    opaque = true
    background = Color.black

    val SpeakerLabel = new Label {
      text = "Speaker: "
      font = new Font(SANS_SERIF, BOLD, 48)
      foreground = Amber
      xAlignment = Alignment.Left
      border = Swing.EmptyBorder(10,10,10,10)
    }
    val ElapsedLabel = new Label {
      text = "0:00"
      font = new Font(MONOSPACED, PLAIN, 200)
      foreground = Color.green
      opaque = true
      xAlignment = Alignment.Center
      yAlignment = Alignment.Center
    }
    val NextButton = new Button {
      text = "Next"
    }
    val GoButton = new Button {
      text = "Go"
    }
    val PauseButton = new Button {
      text = "Pause"
    }
    val SecondsField = new TextField {
      text = ( DefaultLimitMillis / 1000 ).toString
    }
    val SecondsPanel = new BorderPanel {
      val SecondsLabel = new Label {
        text = "seconds"
        font = new Font( SANS_SERIF, PLAIN, 12 )
        xAlignment = Alignment.Left
      }
      layout( SecondsField ) = Center
      layout( SecondsLabel ) = East
    }
    val ActionPanel = new GridPanel(1,4) {
      contents += NextButton
      contents += GoButton
      contents += PauseButton
      contents += SecondsPanel
    }
    layout( SpeakerLabel ) = North
    layout( ElapsedLabel ) = Center
    layout( ActionPanel  ) = South

    def sync() : Unit = {
      val speakerText = "Speaker: " + currentParticipantName.getOrElse("")

      val isStarted  = sessionElapsedMillis > 0
      val isOvertime = sessionElapsedMillis >= sessionLimitMillis

      val elapsedText = {
        if ( isOvertime ) {
          "Time's Up!"
        }
        else {
          val minutes = (sessionElapsedMillis / 1000) / 60
          val seconds = (sessionElapsedMillis / 1000) % 60
          f"$minutes%02d:$seconds%02d"
        }
      }
      SpeakerLabel.text = speakerText
      ElapsedLabel.text = elapsedText

      ElapsedLabel.foreground = {
        if ( sessionElapsedMillis < sessionLimitMillis * FirstWarningFraction )       Color.green
        else if ( sessionElapsedMillis < sessionLimitMillis * SecondWarningFraction ) Color.yellow
        else if ( sessionElapsedMillis < sessionLimitMillis )                         Color.red
        else                                                                          Color.black
      }
      ElapsedLabel.background = if ( isOvertime ) Color.red else Color.black

      val loaded = currentParticipantName.nonEmpty

      NextButton.enabled  = true
      GoButton.enabled    = loaded && (!isStarted || isPaused) && !isOvertime
      PauseButton.enabled = loaded && isStarted && !isPaused && !isOvertime
    }

    sync()
  }
  val TabsPane = new TabbedPane {
    pages += new TabbedPane.Page("Current Speaker", CurrentSpeakerTab)
    pages += new TabbedPane.Page("Participants", EntryTab)
    background = ChromeBackground

    def syncAll() : Unit = {
      ParticipantList.refresh()
      CurrentSpeakerTab.sync()
    }
  }

  def top = new MainFrame {
    title = "Random Seminar";
    contents = TabsPane

    listenTo( EntryField                      )
    listenTo( ParticipantList.keys            )
    listenTo( EntryField                      )
    listenTo( CurrentSpeakerTab.NextButton    )
    listenTo( CurrentSpeakerTab.GoButton      )
    listenTo( CurrentSpeakerTab.PauseButton   )
    listenTo( CurrentSpeakerTab.SecondsField  )

    reactions += {
      case EditDone( EntryField ) => {
        if (EntryField.text.nonEmpty) {
          addParticipant( EntryField.text )
          EntryField.text = ""
          TabsPane.syncAll()
        }
      }
      case EditDone( CurrentSpeakerTab.SecondsField ) => {
        try {
          val secs = CurrentSpeakerTab.SecondsField.text.toInt
          val inTimesUp = ( sessionElapsedMillis >= sessionLimitMillis )
          sessionLimitMillis = secs * 1000
          if (inTimesUp) sessionElapsedMillis = sessionLimitMillis
        }
        catch {
          case nfe : NumberFormatException => CurrentSpeakerTab.SecondsField.text = (sessionLimitMillis / 1000).toString
        }
      }
      case ButtonClicked( CurrentSpeakerTab.NextButton ) => {
        nextParticipant()
      }
      case ButtonClicked( CurrentSpeakerTab.GoButton ) => {
        startSession()
      }
      case ButtonClicked( CurrentSpeakerTab.PauseButton ) => {
        pauseSession()
      }
      case KeyPressed( ParticipantList, Key.BackSpace | Key.Delete, _, _ ) => {
        ParticipantList.selection.items.foreach( p => removeParticipant( p.name ) )
        TabsPane.syncAll()
      }
      case evt =>{
        println(s"Event! ${evt}")
      }
    }

    override def closeOperation() : Unit = {
      Scheduled.attemptCancel()
      AppScheduler.close()
      super.closeOperation()
    }
  }

  private def startSession() : Unit = {
    isPaused = false
  }

  private def pauseSession() : Unit = {
    isPaused = true
  }

  private def nextParticipant() : Unit = this.synchronized {
    val unspokenParticipants = {
      val check = participants.filterNot( _.spokenYet ).toSeq
      if ( check.isEmpty ) {
        val reborn = participants.map( p => p.copy( spokenYet = false ) )
        participants.clear()
        participants ++= reborn
        participants.toVector
      }
      else {
        check.toVector
      }
    }
    if ( unspokenParticipants.nonEmpty ) { // could still be empty if none ave been defined
      val newParticipant = unspokenParticipants( entropy.nextInt( unspokenParticipants.length ) )
      //println( s"Selected participant: ${newParticipant}" )
      removeParticipant( newParticipant.name )
      addParticipant( newParticipant.name, true )
      this.currentParticipantName = Some( newParticipant.name )
    }
    this.isPaused = true
    sessionElapsedMillis = 0L
    ParticipantList.refresh()
  }

  val TickTask = () => {
    Swing.onEDT {
      val now = System.currentTimeMillis()
      if ( !isPaused ) {
        val elapsed = now - lastTickMillis
        sessionElapsedMillis += elapsed
        // println( s"sessionLimitMillis: ${sessionLimitMillis}" )
        sessionElapsedMillis = math.min( sessionLimitMillis, sessionElapsedMillis )
        // println( s"elapsed: $elapsed" )
      }
      lastTickMillis = now
      // println( s"isPaused: $isPaused; lastTickMillis: $lastTickMillis; sessionElapsedMillis: $sessionElapsedMillis" )
      TabsPane.syncAll()
    }
  }

  private val AppScheduler = new Scheduler.withInternalExecutor()
  private val Scheduled    = AppScheduler.scheduleAtFixedRate( TickTask, 0.millis, 200.millis )

  private def addParticipant( name : String, spokenYet : Boolean = false ) : Unit = {
    participants += new Participant( name, spokenYet )
  }

  private def removeParticipant( name : String ) : Unit = {
    participants.retain( _.name != name )
  }
}
