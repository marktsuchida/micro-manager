package org.micromanager.internal.positionlist;

import org.micromanager.MultiStagePosition;
import org.micromanager.internal.utils.ReportingUtils;

class ZInterpolator {
    interface XYToZFunction {
        double call(double x, double y);
    }

    private static class PlaneXYToZFunction implements XYToZFunction {
        private final double x0_;
        private final double y0_;
        private final double z0_;
        private final double bracketX_;
        private final double bracketY_;
        private final double bracketZ_;

        PlaneXYToZFunction(double x0, double y0, double z0,
                           double bracketX, double bracketY, double bracketZ) {
            x0_ = x0;
            y0_ = y0;
            z0_ = z0;
            bracketX_ = bracketX;
            bracketY_ = bracketY;
            bracketZ_ = bracketZ;
        }

        @Override
        public double call(double x, double y) {
            return z0_ -
                    ((x - x0_) * bracketX_ + (y - y0_) * bracketY_) /
                            bracketZ_;
        }
    }

    static XYToZFunction interpolateThreePoint(MultiStagePosition[] points,
                                               String xyLabel,
                                               String zLabel) {
        double x[] = new double[3];
        double y[] = new double[3];
        double z[] = new double[3];
        for (int i = 0; i < 3; ++i) {
            x[i] = points[i].get(xyLabel).x;
            y[i] = points[i].get(xyLabel).y;
            z[i] = points[i].get(zLabel).x;
        }
        double bracketX = (y[1] - y[0]) * (z[2] - z[0]) -
                (z[1] - z[0]) * (y[2] - y[0]);
        double bracketY = (z[1] - z[0]) * (x[2] - x[0]) -
                (x[1] - x[0]) * (z[2] - z[0]);
        double bracketZ = (x[1] - x[0]) * (y[2] - y[0]) -
                (y[1] - y[0]) * (x[2] - x[0]);

        ReportingUtils.logDebugMessage(String.format("bX = %f, bY = %f, bZ = %f",
                bracketX, bracketY, bracketZ));
        if (Math.abs(bracketX) + Math.abs(bracketY) < 1e-6 ||
                Math.abs(bracketZ) < 1e-6) {
            return null;
        }

        return new PlaneXYToZFunction(x[0], y[0], z[0],
                bracketX, bracketY, bracketZ);
    }
}
