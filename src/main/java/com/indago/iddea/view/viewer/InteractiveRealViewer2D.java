package com.indago.iddea.view.viewer;

import com.indago.iddea.view.display.JHotDrawInteractiveDisplay2D;

import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.ui.AffineTransformType2D;

/**
 * Interactive viewer for a 2D using JHotDrawInteractiveDisplay2D {@link RealRandomAccessible}.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 * @author HongKee
 */
public class InteractiveRealViewer2D< T > extends InteractiveRealViewer< T, AffineTransform2D, JHotDrawInteractiveDisplay2D< AffineTransform2D > > {

    /**
     * Create an interactive viewer for a 2D {@link RealRandomAccessible}.
     *
     * @param width
     *            window width.
     * @param height
     *            window height.
     * @param source
     *            The source image to display. It is assumed that the source is
     *            extended to infinity.
     * @param sourceTransform
     *            Transformation from source to global coordinates. This is
     *            useful for pre-scaling when showing anisotropic data, for
     *            example.
     * @param converter
     *            Converter from the source type to argb for rendering the
     */
    public InteractiveRealViewer2D( final int width, final int height, final RealRandomAccessible< T > source, final AffineTransform2D sourceTransform, final Converter< ? super T, ARGBType > converter ) {
        super( AffineTransformType2D.instance, new JHotDrawInteractiveDisplay2D< AffineTransform2D >( width, height, sourceTransform, InteractiveTransformEventHandler2D.factory() ), InjectableDefaults.rendererFactory( AffineTransformType2D.instance, new InjectableSource< T, AffineTransform2D >( source, sourceTransform, converter ) ) );

        this.source = source;
    }
}
