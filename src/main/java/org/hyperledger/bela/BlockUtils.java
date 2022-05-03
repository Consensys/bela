package org.hyperledger.bela;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.hyperledger.bela.model.BlockResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockUtils {
  private static final ObjectMapper mapper = new ObjectMapper()
      .enable(SerializationFeature.INDENT_OUTPUT);;
  private static final Logger LOG = LoggerFactory.getLogger(Bela.class);

  public static String prettyPrintBlockHeader(final BlockResult header) {
    try {
      return mapper.writeValueAsString(header);
    } catch (JsonProcessingException e) {
      LOG.error("error writing block", e);
    }
    return "error writing block";
  }

}
