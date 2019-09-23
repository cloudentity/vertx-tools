package com.cloudentity.tools.vertx.conf;

import com.cloudentity.tools.vertx.logging.InitLog;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.freva.asciitable.HorizontalAlign.CENTER;
import static com.github.freva.asciitable.HorizontalAlign.LEFT;

public class ConfPrinter {
  private static final int MAX_COL_WIDTH = 35;

  public static void logEnvVariables(JsonObject conf) {
    List<ResolvedRef> rows =
      ConfReference.findResolvedEnvRefs(conf).stream()
        .filter(ConfPrinter::filterOutEmptyOptional)
        .collect(Collectors.toList());

    String table =
      AsciiTable.getTable(AsciiTable.FANCY_ASCII, rows,
        Arrays.asList(
          column("STATE").dataAlign(CENTER).with(ref -> evnState(ref)),
          column("ENV").with(ref -> ref.ref.path),
          column("VALUE").with(ref -> ref.resolvedValue != null ? ref.resolvedValue.toString() : "null"),
          column("DEFAULT").with(ref -> ref.ref.defaultValue.orElse("null")),
          column("TYPE").with(ref -> ref.ref.valueType.orElse("string"))
        )
    );

    Arrays.asList(table.split("\n")).forEach(line -> {
      String result =
        line.replaceAll("G<([^>]*)>G", ANSI_GREEN + "  $1  " + ANSI_RESET)
          .replaceAll("Y<([^>]*)>Y", ANSI_YELLOW + "  $1  " + ANSI_RESET)
          .replaceAll("R<([^>]*)>R", ANSI_RED + "  $1  " + ANSI_RESET);
      InitLog.info(result);
    });

    List<String> missingVars = rows.stream().filter(x -> x.resolvedValue == null && !x.ref.optional).map(x -> x.ref.path).collect(Collectors.toList());
    if (!missingVars.isEmpty()) {
      InitLog.error("Missing environment variables: [" + String.join(", ", missingVars) + "]");
    }
  }

  private static boolean filterOutEmptyOptional(ResolvedRef ref) {
    return !ref.ref.optional || ref.resolvedValue != null;
  }

  private static Column column(String label) {
    return new Column().header(label).dataAlign(LEFT).maxColumnWidth(MAX_COL_WIDTH);
  }

  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_GREEN = "\u001B[32m";
  private static final String ANSI_YELLOW = "\u001B[33m";
  private static final String ANSI_RED = "\u001B[31m";

  private static String evnState(ResolvedRef ref) {
    if (ref.ref.defaultValue.isPresent()) {
      String resolvedValue = Optional.ofNullable(ref.resolvedValue).map(Object::toString).orElse(null);
      if (!ref.ref.defaultValue.get().equals(resolvedValue)) {
        return "G<OVERWRITE>G"; // if default value overridden
      } else {
        return "Y<DEFAULT>Y"; // if default value not overridden
      }
    } else {
      if (ref.resolvedValue == null) {
        return ref.ref.optional ? "  OPTIONAL" : "R<MISSING>R"; // if no default value and env variable not set
      } else {
        return "G<SET>G"; // if no default value and env variable set
      }
    }
  }

  public static void logMetaConfigEnvVariables(JsonObject conf) {
    InitLog.info("Environment variables in meta config: ");
    conf.getJsonArray("stores").forEach(store -> {
      if (store instanceof JsonObject) {
        JsonObject obj = (JsonObject) store;
        InitLog.info("- " + ((JsonObject) store).getString("type"));
        ConfPrinter.logEnvVariables(obj);
      }
    });
  }
}
