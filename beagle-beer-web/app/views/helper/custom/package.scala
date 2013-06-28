package views.html.helper

/**
 * Exposes the custom FieldConstructors used by this application
 */
package object custom {

  implicit def searchStyleInputWithButton(buttonName: String) = new FieldConstructor {
    def apply(elts: FieldElements) = searchStyleInputWithButtonFieldConstructor(elts, buttonName)
  }

  implicit val tableFieldNoLabel = new FieldConstructor {
    def apply(elts: FieldElements) = tableInputNoLabelFieldConstructor(elts)
  }
}


