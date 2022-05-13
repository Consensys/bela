package org.hyperledger.bela.components;

import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.bela.model.TransactionResult;

public class TransactionPanel implements LanternaComponent<Panel> {
  private static final int MAX_VALUE_WIDTH = 60;
  private static final int MAX_LABEL_WIDTH = 20;

  private final Label hash = new Label("empty").setLabelWidth(MAX_VALUE_WIDTH);
  private final Label nonce = new Label("empty").setLabelWidth(MAX_VALUE_WIDTH);
  private final Label gasPrice = new Label("empty").setLabelWidth(MAX_VALUE_WIDTH);
  private final Label gasLimit = new Label("empty").setLabelWidth(MAX_VALUE_WIDTH);
  private final Label to = new Label("empty").setLabelWidth(MAX_VALUE_WIDTH);
  private final Label sender = new Label("empty").setLabelWidth(MAX_VALUE_WIDTH);
  private final Label value = new Label("empty").setLabelWidth(MAX_VALUE_WIDTH);
  private final Label payload = new Label("empty").setLabelWidth(MAX_VALUE_WIDTH);
  private final Label chainId = new Label("empty").setLabelWidth(MAX_VALUE_WIDTH);
  private final Label signature = new Label("empty").setLabelWidth(MAX_VALUE_WIDTH);
  private final Label maxPriorityFeePerGas = new Label("empty").setLabelWidth(MAX_VALUE_WIDTH);
  private final Label maxFeePerGas = new Label("empty").setLabelWidth(MAX_VALUE_WIDTH);

  public TransactionPanel(final TransactionResult transactionResult) {
    updateWithTransaction(transactionResult);
  }

  @Override
  public Panel createComponent() {
    Panel panel = new Panel();
    panel.setLayoutManager(new GridLayout(2));

    panel.addComponent(new Label("Hash:"));
    panel.addComponent(hash);
    panel.addComponent(new Label("Nonce:"));
    panel.addComponent(nonce);
    panel.addComponent(new Label("Gas Price:"));
    panel.addComponent(gasPrice);
    panel.addComponent(new Label("Gas Limit:"));
    panel.addComponent(gasLimit);
    panel.addComponent(new Label("Max Priority Fee Per Gas:"));
    panel.addComponent(maxPriorityFeePerGas);
    panel.addComponent(new Label("Max Fee Per Gas:"));
    panel.addComponent(maxFeePerGas);
    panel.addComponent(new Label("To:"));
    panel.addComponent(to);
    panel.addComponent(new Label("Sender:"));
    panel.addComponent(sender);
    panel.addComponent(new Label("Value:"));
    panel.addComponent(value);
    panel.addComponent(new Label("Payload:"));
    panel.addComponent(payload);
    panel.addComponent(new Label("ChainId:"));
    panel.addComponent(chainId);
    panel.addComponent(new Label("Signature:"));
    panel.addComponent(signature);

    return panel;
  }

  public void updateWithTransaction(final TransactionResult transactionResult) {
    hash.setText(transactionResult.getHash());
    nonce.setText(String.valueOf(transactionResult.getNonce()));
    gasPrice.setText(transactionResult.getGasPrice());
    gasLimit.setText(String.valueOf(transactionResult.getGasLimit()));
    maxPriorityFeePerGas.setText(transactionResult.getMaxPriorityFeePerGas());
    maxFeePerGas.setText(transactionResult.getMaxFeePerGas());
    to.setText(transactionResult.getTo());
    sender.setText(transactionResult.getSender());
    value.setText(transactionResult.getValue());
    payload.setText(transactionResult.getPayload());
    chainId.setText(transactionResult.getChainId());
    signature.setText(transactionResult.getSignature());
  }
}
