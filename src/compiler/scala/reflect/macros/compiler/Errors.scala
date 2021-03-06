package scala.reflect.macros
package compiler

import scala.compat.Platform.EOL
import scala.reflect.macros.util.Traces

trait Errors extends Traces {
  self: DefaultMacroCompiler =>

  import global._
  import analyzer._
  import definitions._
  import typer.TyperErrorGen._
  import typer.infer.InferErrorGen._
  def globalSettings = global.settings

  // sanity check errors

  private def implRefError(message: String) = c.abort(macroDdef.pos, message)

  def MacroImplReferenceWrongShapeError() = implRefError(
      "macro implementation reference has wrong shape. required:\n"+
      "macro [<static object>].<method name>[[<type args>]] or\n" +
      "macro [<macro bundle>].<method name>[[<type args>]]")

  def MacroImplNotPublicError() = implRefError("macro implementation must be public")

  def MacroImplOverloadedError() = implRefError("macro implementation cannot be overloaded")

  def MacroImplWrongNumberOfTypeArgumentsError() = implRefError(TypedApplyWrongNumberOfTpeParametersErrorMessage(macroImplRef))

  // compatibility errors

  // helpers

  private def lengthMsg(flavor: String, violation: String, extra: Symbol) = {
    val noun = if (flavor == "value") "parameter" else "type parameter"
    val message = noun + " lists have different length, " + violation + " extra " + noun
    val suffix = if (extra ne NoSymbol) " " + extra.defString else ""
    message + suffix
  }

  private def abbreviateCoreAliases(s: String): String = List("WeakTypeTag", "Expr").foldLeft(s)((res, x) => res.replace("c.universe." + x, "c." + x))

  private def showMeth(pss: List[List[Symbol]], restpe: Type, abbreviate: Boolean) = {
    var argsPart = (pss map (ps => ps map (_.defString) mkString ("(", ", ", ")"))).mkString
    if (abbreviate) argsPart = abbreviateCoreAliases(argsPart)
    var retPart = restpe.toString
    if (abbreviate || macroDdef.tpt.tpe == null) retPart = abbreviateCoreAliases(retPart)
    argsPart + ": " + retPart
  }

  // not exactly an error generator, but very related
  // and I dearly wanted to push it away from Macros.scala
  private def checkConforms(slot: String, rtpe: Type, atpe: Type) = {
    val verbose = macroDebugVerbose || settings.explaintypes.value

    def check(rtpe: Type, atpe: Type): Boolean = {
      def prematureOk() = { if (verbose) println(rtpe + " <: " + atpe + "?" + EOL + "true"); true }
      (rtpe, atpe) match {
        case _ if rtpe eq atpe => prematureOk()
        case (TypeRef(_, RepeatedParamClass, rtpe :: Nil), TypeRef(_, RepeatedParamClass, atpe :: Nil)) => check(rtpe, atpe)
        case (ExprClassOf(_), TreeType()) => prematureOk()
        case (TreeType(), ExprClassOf(_)) => prematureOk()
        case _ => rtpe <:< atpe
      }
    }

    val ok =
      if (verbose) withTypesExplained(check(rtpe, atpe))
      else check(rtpe, atpe)
    if (!ok) {
      compatibilityError("type mismatch for %s: %s does not conform to %s".format(slot, abbreviateCoreAliases(rtpe.toString), abbreviateCoreAliases(atpe.toString)))
    }
  }

  private def compatibilityError(message: String) =
    implRefError(
      "macro implementation has wrong shape:"+
      "\n required: " + showMeth(rparamss, rret, abbreviate = true) +
      "\n found   : " + showMeth(aparamss, aret, abbreviate = false) +
      "\n" + message)

  def MacroImplNonTagImplicitParameters(params: List[Symbol]) = compatibilityError("macro implementations cannot have implicit parameters other than WeakTypeTag evidences")

  def MacroImplParamssMismatchError() = compatibilityError("number of parameter sections differ")

  def MacroImplExtraParamsError(aparams: List[Symbol], rparams: List[Symbol]) = compatibilityError(lengthMsg("value", "found", aparams(rparams.length)))

  def MacroImplMissingParamsError(aparams: List[Symbol], rparams: List[Symbol]) = compatibilityError(abbreviateCoreAliases(lengthMsg("value", "required", rparams(aparams.length))))

  def checkMacroImplParamTypeMismatch(atpe: Type, rparam: Symbol) = checkConforms("parameter " + rparam.name, rparam.tpe, atpe)

  def checkMacroImplResultTypeMismatch(atpe: Type, rret: Type) = checkConforms("return type", atpe, rret)

  def MacroImplParamNameMismatchError(aparam: Symbol, rparam: Symbol) = compatibilityError("parameter names differ: " + rparam.name + " != " + aparam.name)

  def MacroImplVarargMismatchError(aparam: Symbol, rparam: Symbol) = {
    if (isRepeated(rparam) && !isRepeated(aparam))
      compatibilityError("types incompatible for parameter " + rparam.name + ": corresponding is not a vararg parameter")
    if (!isRepeated(rparam) && isRepeated(aparam))
      compatibilityError("types incompatible for parameter " + aparam.name + ": corresponding is not a vararg parameter")
  }

  def MacroImplTargMismatchError(atargs: List[Type], atparams: List[Symbol]) =
    compatibilityError(NotWithinBoundsErrorMessage("", atargs, atparams, macroDebugVerbose || settings.explaintypes.value))

  def MacroImplTparamInstantiationError(atparams: List[Symbol], ex: NoInstance) =
    compatibilityError(
      "type parameters "+(atparams map (_.defString) mkString ", ")+" cannot be instantiated\n"+
      ex.getMessage)
}