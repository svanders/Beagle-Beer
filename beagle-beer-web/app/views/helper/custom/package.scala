package views.html.helper

/**
 * Created with IntelliJ IDEA.
 * User: simonvandersluis
 * Date: 25/06/13
 * Time: 4:36 PM
 * To change this template use File | Settings | File Templates.
 */
//object CustomHelpers {

//  implicit val myFields = FieldConstructor(spannedOneLineInput.f)

  package object custom {
    implicit val spannedOnlyField = new FieldConstructor {
      def apply(elts: FieldElements) = spannedOneLineInput(elts)
    }

    implicit val tableFieldNoLabel = new FieldConstructor {
      def apply(elts: FieldElements) = tableInputNoLabel(elts)
    }
  }

//}

