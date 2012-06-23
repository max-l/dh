package models.ev


case class Ballotz(title: String, instructions: String, 
    candidates: Seq[(String, Int)])

case class Argument(title: String, start: String, rest: String)


object Main {

  def evaluativeVoting = 
    fr("Vote Évaluatif") ~ 
    en("Evaluative Voting")


  val ballotFr = Ballotz(
      "Bulltetin de Vote",
      "Votez pour chaque candidat que vous appouvez, le gagnant sera celui qui obtiendra le plus de vote.",
       Seq(
         "Le Parrain" -> -2,
         "Obelix" -> 1,
         "Gaston Lagaffe" -> -1,
         "Asterix" -> 2
       ))

  val ballotEn = Ballotz(
      "Voting Ballot",
      "Vote for as many candidate as you approve, the one with the most vote wins.",
       Seq(
         "Le Parrain" -> -2,
         "Obelix" -> -1,
         "Gaston Lagaffe" -> 0,
         "Gandalf" -> 3
       ))

  def convertToAv(b: Ballotz) =
    b.copy(candidates = b.candidates.map(c => (c._1, if(c._2 > 0) 1 else 0)))
       
  def exampleAVBallot = 
     fr.custom(convertToAv(ballotFr)) ~
     en.custom(convertToAv(ballotEn))
  
  def exampleRVBallot =
     fr.custom(ballotFr.copy(instructions = 
       "Évaluez chaque candidat avec un score <br> 2: excellent, 1: bon, 0: neutre, -1: mauvais, -2: très mauvais")) ~
     en.custom(ballotEn.copy(instructions = 
       "Evaluate each candidat with a grade <br> 2: Excellent, 1: Good, 0: Neutral, -1: Bad, -2: Very Bad"))

  def pop(s: String, id: String) = <a id={id}>{s}</a>

  def fbLang =
    fr("fr_CA") ~ en("en_CA")  
}