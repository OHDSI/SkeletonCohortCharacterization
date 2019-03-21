#@file Main.R

#' SkeletonCohortCharacterization
#'
#' @docType package
#' @name SkeletonCohortCharacterization
NULL

.onLoad <- function(libname, pkgname) {
  rJava::.jpackage(pkgname, lib.loc = libname)
}
