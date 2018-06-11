package org.micromanager.internal.positionlist;

import mmcorej.CMMCore;
import mmcorej.MMCoreJ;
import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;
import org.micromanager.MultiStagePosition;
import org.micromanager.PositionList;
import org.micromanager.StagePosition;
import org.micromanager.Studio;
import org.micromanager.internal.utils.MMDialog;
import org.micromanager.internal.utils.NumberUtils;
import org.micromanager.internal.utils.ReportingUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

final class TileEachDlg extends MMDialog {
    final CMMCore core_;
    final Studio studio_;
    final PositionList sourcePositions_;
    final List<Integer> selectedPositionIndices_;

    final JSpinner nRowsSpinner_;
    final JSpinner nColsSpinner_;
    final JLabel infoLabel_;
    final JTextField overlapField_;
    final JComboBox overlapUnitComboBox_;
    final JTextField pixelSizeField_;
    final JButton okButton_;
    final JButton cancelButton_;

    String overlapUnit_ = "um";

    int rows_ = 0;
    int cols_ = 0;
    double fieldWidthUm_ = 0.0;
    double fieldHeightUm_ = 0.0;
    double hSpacingUm_ = 0.0;
    double vSpacingUm_ = 0.0;
    double totalWidth_ = 0.0;
    double totalHeight_ = 0.0;

    TileEachDlg(CMMCore core, Studio studio,
                PositionList positions, List<Integer> selectedIndices) {
        super("tile each position dialog");

        core_ = core;
        studio_ = studio;
        sourcePositions_ = positions;
        selectedPositionIndices_ = selectedIndices;

        setModal(true);
        setResizable(false);
        setTitle("Tile Around Each Position");
        super.loadAndRestorePosition(300, 300);

        nRowsSpinner_ = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));
        nRowsSpinner_.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                recompute();
            }
        });

        nColsSpinner_ = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));
        nColsSpinner_.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                recompute();
            }
        });

        infoLabel_ = new JLabel();
        infoLabel_.setHorizontalAlignment(JLabel.CENTER);

        overlapField_ = new JTextField(6);
        overlapField_.setText("0");
        overlapField_.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                recompute();
            }
        });

        overlapUnitComboBox_ = new JComboBox(new String[] { "um", "px", "%" });
        overlapUnitComboBox_.setSelectedIndex(0);
        overlapUnitComboBox_.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                overlapUnit_ = (String) overlapUnitComboBox_.getSelectedItem();
                recompute();
            }
        });

        pixelSizeField_ = new JTextField(6);
        pixelSizeField_.setText(NumberUtils.doubleToDisplayString(core_.getPixelSizeUm()));
        pixelSizeField_.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                recompute();
            }
        });

        okButton_ = new JButton("OK");
        okButton_.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TileEachDlg.this.dispose();
                applyToPositionList();
            }
        });

        cancelButton_ = new JButton("Cancel");
        cancelButton_.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TileEachDlg.this.dispose();
            }
        });

        getContentPane().setLayout(new MigLayout());

        add(new JLabel("Columns:"), new CC().alignX("label").gapAfter("rel"));
        add(nColsSpinner_, new CC().wrap());

        add(new JLabel("Rows:"), new CC().alignX("label").gapAfter("rel"));
        add(nRowsSpinner_, new CC().wrap());

        add(infoLabel_, new CC().span().growX().wrap());

        add(new JLabel("Overlap:"), new CC().alignX("label").gapAfter("rel"));
        add(overlapField_, new CC().split().gapAfter("0"));
        add(overlapUnitComboBox_, new CC().wrap());

        add(new JLabel("Pixel Size:"), new CC().alignX("label").gapAfter("rel"));
        add(pixelSizeField_, new CC().split().gapAfter("0"));
        add(new JLabel("um"), new CC().wrap().gapBottom("unrelated"));

        add(okButton_, new CC().span().split().tag("ok").
                gapBefore("push").gapAfter("rel"));
        add(cancelButton_, new CC().tag("cancel"));

        recompute();
        pack();
    }

    private void recompute() {
        String camera = core_.getCameraDevice();
        if (camera == null || camera.isEmpty()) {
            infoLabel_.setText("No camera configured");
            okButton_.setEnabled(false);
            return;
        }

        boolean isXYSwapped;
        try {
            String correction = core_.getProperty(camera, "TransposeCorrection");
            String transpose = core_.getProperty(camera, MMCoreJ.getG_Keyword_Transpose_SwapXY());
            isXYSwapped = correction.equals("0") && !transpose.equals("0");
        }
        catch (Exception e) {
            isXYSwapped = false;
        }

        rows_ = (Integer) nRowsSpinner_.getValue();
        cols_ = (Integer) nColsSpinner_.getValue();

        double pixelSizeUm;
        try {
            pixelSizeUm = NumberUtils.displayStringToDouble(pixelSizeField_.getText());
        }
        catch (ParseException e) {
            infoLabel_.setText("Invalid pixel size");
            okButton_.setEnabled(false);
            return;
        }
        if (pixelSizeUm <= 0.0) {
            infoLabel_.setText("Negative pixel size");
            okButton_.setEnabled(false);
            return;
        }

        fieldWidthUm_ = core_.getImageWidth() * pixelSizeUm;
        fieldHeightUm_ = core_.getImageHeight() * pixelSizeUm;
        if (isXYSwapped) {
            double wk = fieldWidthUm_;
            fieldWidthUm_ = fieldHeightUm_;
            fieldHeightUm_ = wk;
        }

        double overlapValue;
        try {
            overlapValue = NumberUtils.displayStringToDouble(overlapField_.getText());
        }
        catch (ParseException e) {
            infoLabel_.setText(("Invalid unit for overlap"));
            okButton_.setEnabled(false);
            return;
        }

        String overlapUnit = (String) overlapUnitComboBox_.getSelectedItem();
        double hOverlapUm, vOverlapUm;
        if (overlapUnit.equals("um")) {
            hOverlapUm = vOverlapUm = overlapValue;
        }
        else if (overlapUnit.equals("px")) {
            hOverlapUm = vOverlapUm = overlapValue * pixelSizeUm;
        }
        else if (overlapUnit.equals("%")) {
            hOverlapUm = fieldWidthUm_ * overlapValue / 100.0;
            vOverlapUm = fieldHeightUm_ * overlapValue / 100.0;
        }
        else {
            infoLabel_.setText("Unexpected error");
            okButton_.setEnabled(false);
            return;
        }

        hSpacingUm_ = fieldWidthUm_ - hOverlapUm;
        vSpacingUm_ = fieldHeightUm_ - vOverlapUm;

        totalWidth_ = fieldWidthUm_ * cols_ - hOverlapUm * (cols_ - 1);
        totalHeight_ = fieldHeightUm_ * rows_ - vOverlapUm * (rows_ - 1);

        infoLabel_.setText(String.format("%s x %s um",
                NumberUtils.doubleToDisplayString(totalWidth_),
                NumberUtils.doubleToDisplayString(totalHeight_)));
        okButton_.setEnabled(true);
    }

    private void applyToPositionList() {
        String xyStage = core_.getXYStageDevice();
        if (xyStage == null || xyStage.isEmpty()) {
            ReportingUtils.showError("No XY stage configured", this);
            return;
        }

        PositionList dest = new PositionList();
        for (int i = 0; i < sourcePositions_.getNumberOfPositions(); ++i) {
            MultiStagePosition msp = sourcePositions_.getPosition(i);
            if (selectedPositionIndices_.contains(i)) {
                addGrid(dest, msp, xyStage);
            }
            else {
                dest.addPosition(msp);
            }
        }
        studio_.positions().setPositionList(dest);
    }

    private void addGrid(PositionList dest, MultiStagePosition center,
                         String xyStage) {
        String label = center.getLabel();
        StagePosition xy = center.get(xyStage);
        if (xy == null) {
            ReportingUtils.showError(String.format("Position %s has no XY coordinates"));
            return;
        }

        double startX = xy.x - 0.5 * totalWidth_ + 0.5 * fieldWidthUm_;
        double startY = xy.y - 0.5 * totalHeight_ + 0.5 * fieldHeightUm_;

        for (int r = 0; r < rows_; ++r) {
            for (int c0 = 0; c0 < cols_; ++c0) {
                int c = c0;
                if (r % 2 != 0) { // Serpentine order
                    c = cols_ - 1 - c0;
                }

                double x = startX + c * hSpacingUm_;
                double y = startY + r * vSpacingUm_;
                MultiStagePosition msp = MultiStagePosition.newInstance(center);
                msp.setLabel(String.format("%s-%d-%d", label, c, r));
                StagePosition sp = msp.get(xyStage);
                sp.x = x;
                sp.y = y;
                dest.addPosition(msp);
            }
        }
    }
}
